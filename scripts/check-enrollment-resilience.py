"""ENROLL-09 on the disposable Compose overlay; never run against a demo stack.

Requires confluent-kafka==2.6.1. Tokens stay in memory. Evidence contains only QA
course events, source/DLT offsets, allowlisted headers and assertion outcomes.
"""

import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import time
import urllib.error
import urllib.request
import uuid
from datetime import datetime, timezone

from confluent_kafka import Consumer, ConsumerGroupTopicPartitions, KafkaError, KafkaException, Producer, TopicPartition
from confluent_kafka.admin import (AdminClient, AclBinding, AclBindingFilter, AclOperation,
                                  AclPermissionType, NewTopic, ResourcePatternType, ResourceType)


TOPIC = "elearning.course.events"
DLT = TOPIC + ".DLT"
GROUP = "enrollment-service"
DB = "enrollment-resilience-db"
COMPOSE = ["docker", "compose", "--profile", "app"]
OUTPUT = Path("target/enrollment-resilience/recovery.json")


def now():
    return datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")


def require(condition, message):
    if not condition:
        raise AssertionError(message)


def command(*args, env=None, timeout=120):
    result = subprocess.run(args, text=True, capture_output=True, timeout=timeout, env=env)
    if result.returncode:
        # Do not echo commands, environment, HTTP bodies or unrestricted service logs.
        raise RuntimeError(f"Command {args[0]} failed (exit {result.returncode})")
    return result.stdout.strip()


def compose(*args, **kwargs):
    return command(*COMPOSE, *args, **kwargs)


def wait_for(label, check, timeout=60, interval=1):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        result = check()
        if result:
            return result
        time.sleep(interval)
    raise AssertionError(f"Timed out: {label}")


def sql(statement):
    require(statement.startswith("SELECT "), "Test database inspection must be read-only")
    return compose("exec", "-T", DB, "sh", "-c",
                   'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u"$MYSQL_USER" -N -B enrollment_db -e "$1"',
                   "sh", statement, timeout=15)


def snapshot(course_id):
    raw = sql("SELECT JSON_OBJECT('courseId', course_id, 'title', title, 'status', status, "
              "'totalLessons', total_lessons) FROM course_snapshots "
              f"WHERE course_id = {int(course_id)}")
    return json.loads(raw) if raw else None


def expect_snapshot(event):
    def matches():
        row = snapshot(event["courseId"])
        return row if row and all(row[k] == event[k] for k in row) else None
    return wait_for("snapshot matches course event", matches, timeout=100)


def db_start():
    compose("up", "-d", "--no-deps", "--wait", "--wait-timeout", "90", DB)


def retry_budget(value):
    env = dict(os.environ, ENROLLMENT_TEST_RETRY_BUDGET=value)
    compose("up", "-d", "--no-deps", "--wait", "--wait-timeout", "120",
            "enrollment-service", env=env, timeout=150)


def api(method, path, body=None, token=None, expected=200):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    request = urllib.request.Request("http://localhost:8080" + path,
                                     data=json.dumps(body).encode() if body is not None else None,
                                     headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=15) as response:
            require(response.status == expected, f"{method} {path}: unexpected HTTP {response.status}")
            return json.load(response)["data"]
    except urllib.error.HTTPError as error:
        raise AssertionError(f"{method} {path}: HTTP {error.code}, expected {expected}") from None


class Probe:
    def __init__(self):
        self.run_id = uuid.uuid4().hex
        self.next_id = 8_000_000_000_000 + int(self.run_id[:8], 16)
        self.admin = AdminClient({"bootstrap.servers": "localhost:9092"})
        self.producer = Producer({"bootstrap.servers": "localhost:9092", "acks": "all",
                                  "delivery.timeout.ms": 15000})
        self.readers = []
        self.acls_changed = False
        self.dlt_acls = [AclBinding(ResourceType.TOPIC, DLT, ResourcePatternType.LITERAL,
                                   "User:ANONYMOUS", "*", operation, permission)
                         for operation, permission in [(AclOperation.ALL, AclPermissionType.ALLOW),
                                                        (AclOperation.WRITE, AclPermissionType.DENY)]]
        self.evidence = {"sourceCommit": os.environ.get("SOURCE_COMMIT"),
                         "startedAt": now(), "database": "MySQL 8.4 (dedicated enrollment DB)",
                         "broker": "Kafka 4.3.1", "cases": [], "cleanup": {}}

    def event(self, title="Resilience QA"):
        self.next_id += 1
        return {"eventType": "course.updated", "eventId": str(uuid.uuid4()), "occurredAt": now(),
                "courseId": self.next_id, "title": title, "slug": f"qa-{self.next_id}",
                "thumbnailUrl": None, "instructorId": 1, "instructorName": "Resilience QA",
                "totalLessons": 2, "status": "PUBLISHED"}

    def prepare(self):
        require(os.environ.get("ENROLLMENT_RESILIENCE_DISPOSABLE") == "1",
                "Requires explicit disposable-stack flag")
        require(os.environ.get("COMPOSE_PROJECT_NAME") == "enrollment-resilience",
                "Requires the enrollment-resilience Compose project")
        require("infra/enrollment-resilience.compose.yml" in os.environ.get("COMPOSE_FILE", ""),
                "Requires the test-only Compose overlay")
        require(os.environ.get("ENROLLMENT_TEST_RETRY_BUDGET", "5m") == "5m",
                "Start with the production retry budget (5m)")
        # Fail before any outage if this is not the dedicated test database.
        for service in (DB, "enrollment-service", "kafka", "api-gateway"):
            container_id = compose("ps", "-q", service)
            require(bool(container_id), f"Test service is not running: {service}")
            project = command("docker", "inspect", "--format",
                              '{{index .Config.Labels "com.docker.compose.project"}}', container_id)
            require(project == "enrollment-resilience", f"Unexpected project for {service}")
        enrollment = json.loads(command("docker", "inspect", compose("ps", "-q", "enrollment-service")))[0]
        require("DB_HOST=" + DB in enrollment["Config"]["Env"], "Enrollment is not using dedicated MySQL")
        metadata = self.admin.list_topics(timeout=15)
        require(TOPIC in metadata.topics, "Run demo-flow before this test to create the course topic")
        if DLT not in metadata.topics:
            self.admin.create_topics([NewTopic(DLT, num_partitions=3, replication_factor=1)])[DLT].result(15)
        acl_filter = AclBindingFilter(ResourceType.TOPIC, DLT, ResourcePatternType.MATCH,
                                     None, None, AclOperation.ANY, AclPermissionType.ANY)
        require(not self.admin.describe_acls(acl_filter).result(15), "DLT already has ACLs; refuse to modify them")
        self.dlt_reader = self.reader(DLT)
        self.source_reader = self.reader(TOPIC)
        self.barrier()

    def reader(self, topic):
        consumer = Consumer({"bootstrap.servers": "localhost:9092", "group.id": "qa-" + uuid.uuid4().hex,
                             "enable.auto.commit": False, "auto.offset.reset": "latest"})
        transient_metadata_errors = (KafkaError.NOT_LEADER_FOR_PARTITION,
                                     KafkaError.LEADER_NOT_AVAILABLE, KafkaError.UNKNOWN_TOPIC_OR_PART)

        def topic_partitions():
            metadata = self.admin.list_topics(topic, timeout=10).topics.get(topic)
            if metadata is None:
                return None
            if metadata.error:
                if metadata.error.code() in transient_metadata_errors:
                    return None
                raise KafkaException(metadata.error)
            return metadata.partitions or None

        partitions = wait_for("new topic metadata available", topic_partitions, timeout=30)
        assignments = []
        for partition in partitions:
            def watermark():
                try:
                    return consumer.get_watermark_offsets(TopicPartition(topic, partition), timeout=5)
                except KafkaException as error:
                    # CreateTopics can finish before every client's metadata sees
                    # the new leader. Retry only these startup metadata errors.
                    if error.args[0].code() in transient_metadata_errors:
                        consumer.list_topics(topic, timeout=5)
                        return None
                    raise
            _, end = wait_for("new topic partition leader ready", watermark, timeout=30)
            assignments.append(TopicPartition(topic, partition, end))
        consumer.assign(assignments)
        self.readers.append(consumer)
        return consumer

    def send(self, event=None, raw=None, key=None, partition=0):
        payload = raw if raw is not None else json.dumps(event, separators=(",", ":"))
        key = str(key if key is not None else event["courseId"])
        delivered = []
        self.producer.produce(TOPIC, key=key, value=payload, partition=partition,
                              headers={"enrollment-qa-id": self.run_id},
                              on_delivery=lambda error, message: delivered.append((error, message)))
        require(self.producer.flush(20) == 0 and bool(delivered), "QA source event was not delivered")
        error, message = delivered[0]
        require(error is None, "Broker rejected QA source event")
        return {"topic": TOPIC, "partition": message.partition(), "offset": message.offset(),
                "key": key, "payload": payload, "qaHeader": self.run_id}

    def committed(self, partition):
        request = ConsumerGroupTopicPartitions(GROUP, [TopicPartition(TOPIC, partition)])
        result = self.admin.list_consumer_group_offsets([request])[GROUP].result(15)
        return result.topic_partitions[0].offset

    def consumed(self, source):
        wait_for("source offset committed", lambda: self.committed(source["partition"]) > source["offset"])
        return self.committed(source["partition"])

    def barrier(self):
        event = self.event("Ready before outage")
        source = self.send(event)
        expect_snapshot(event)
        self.consumed(source)

    def dlt(self, source, timeout=60):
        deadline = time.monotonic() + timeout
        other_groups = set()
        while time.monotonic() < deadline:
            message = self.dlt_reader.poll(1)
            if message is None:
                continue
            require(message.error() is None, "DLT reader failed")
            if message.key() != source["key"].encode() or message.value() != source["payload"].encode():
                continue
            headers = dict(message.headers() or [])
            # notification-service consumes the same topic and uses the same DLT.
            # Its copy can arrive first, especially after restoring DLT access.
            group = headers.get("kafka_dlt-original-consumer-group")
            if group != GROUP.encode():
                other_groups.add(group.decode(errors="replace") if group else "<missing>")
                continue
            require(headers.get("kafka_dlt-original-topic") == TOPIC.encode(), "DLT original topic mismatch")
            for suffix, value in [("partition", source["partition"]), ("offset", source["offset"])]:
                raw = headers.get("kafka_dlt-original-" + suffix)
                require(raw is not None and int.from_bytes(raw, "big", signed=True) == value,
                        "DLT original " + suffix + " mismatch")
            require(bool(headers.get("kafka_dlt-exception-fqcn")), "DLT exception header missing")
            if source.get("qaHeader"):
                require(headers.get("enrollment-qa-id") == self.run_id.encode(), "QA source header lost")
            return {"topic": DLT, "partition": message.partition(), "offset": message.offset(),
                    "key": source["key"], "payload": source["payload"],
                    "payloadSha256": hashlib.sha256(message.value()).hexdigest(),
                    "originalTopic": TOPIC, "originalPartition": source["partition"],
                    "originalOffset": source["offset"], "originalConsumerGroup": GROUP,
                    "otherConsumerGroupsObserved": sorted(other_groups),
                    "exceptionClass": headers["kafka_dlt-exception-fqcn"].decode(),
                    "sourceHeaderPreserved": bool(source.get("qaHeader"))}
        raise AssertionError(f"Expected enrollment payload/key/headers in DLT; other groups: {sorted(other_groups)}")

    def source_from_course(self, course_id):
        def find():
            message = self.source_reader.poll(1)
            if message is None:
                return None
            require(message.error() is None, "Course event reader failed")
            if message.key() != str(course_id).encode():
                return None
            event = json.loads(message.value())
            if event.get("status") != "PUBLISHED":
                return None
            return {"topic": TOPIC, "partition": message.partition(), "offset": message.offset(),
                    "key": str(course_id), "payload": message.value().decode()}
        return wait_for("course-service published event during enrollment DB outage", find, timeout=30)

    def logs(self, since):
        return compose("logs", "--no-color", "--since", since, "enrollment-service", timeout=15)

    def database_failures(self, since):
        # Count connection failures on the Kafka listener, not the scheduled outbox worker.
        # Hibernate 7.4 reports JDBC errors at WARN; older versions used ERROR.
        return sum("snapshots-0-C-1" in line and "Connection is not available" in line
                   and re.search(r"\b(?:WARN|ERROR)\b", line) is not None
                   for line in self.logs(since).splitlines())

    def case_one(self):
        token = api("POST", "/api/auth/login", {"email": "admin@elearning.hunre.edu.vn",
                                                "password": "Admin@123456"})["accessToken"]
        category = api("POST", "/api/categories", {"name": "Resilience " + self.run_id,
                                                   "slug": "resilience-" + self.run_id}, token, 201)
        course = api("POST", "/api/courses", {"categoryId": category["id"],
                     "title": "ENROLL-09 recovery " + self.run_id, "slug": "recovery-" + self.run_id,
                     "price": 0, "level": "BEGINNER", "language": "vi"}, token, 201)
        section = api("POST", f'/api/courses/{course["id"]}/sections',
                      {"title": "Recovery", "position": 0}, token, 201)
        api("POST", f'/api/sections/{section["id"]}/lessons',
            {"title": "Lesson", "type": "ARTICLE", "content": "QA", "position": 0}, token, 201)
        require(snapshot(course["id"]) is None, "Draft should not already have a snapshot")
        since = now()
        compose("stop", "-t", "5", DB)
        outage_start = time.monotonic()
        try:
            api("PATCH", f'/api/courses/{course["id"]}/status', {"status": "PUBLISHED"}, token)
            source = self.source_from_course(course["id"])
            observations = []
            while time.monotonic() - outage_start < 40:
                offset = self.committed(source["partition"])
                require(offset <= source["offset"], "Offset advanced while enrollment MySQL was down")
                observations.append({"seconds": round(time.monotonic() - outage_start, 1), "committed": offset})
                time.sleep(2)
            failures = self.database_failures(since)
            self.evidence["cases"][-1]["evidence"] = {
                "source": source, "offsetsDuringOutage": observations, "databaseFailureCount": failures}
            require(failures >= 2, "Did not observe repeated real database connection failures")
        finally:
            duration = round(time.monotonic() - outage_start, 1)
            db_start()
        row = expect_snapshot(json.loads(source["payload"]))
        committed = self.consumed(source)
        # Reading all currently available DLT records also catches an incorrect recovery policy.
        deadline = time.monotonic() + 3
        while time.monotonic() < deadline:
            message = self.dlt_reader.poll(0.5)
            if message is not None:
                require(message.error() is None, "DLT reader failed")
                require(message.value() != source["payload"].encode(), "Transient outage event went to DLT")
        return {"outageSeconds": duration, "publishHttp": 200, "source": source,
                "offsetsDuringOutage": observations, "databaseFailureCount": failures,
                "snapshot": row, "committedAfterRecovery": committed, "noDeadLetter": True}

    def invalid_case(self, oversized):
        event = self.event("x" * 300 if oversized else "Malformed JSON")
        source = self.send(event) if oversized else self.send(raw="not-json", key=event["courseId"])
        following = self.event("After invalid event")
        next_source = self.send(following, partition=source["partition"])
        require(next_source["offset"] == source["offset"] + 1, "Following record is not adjacent")
        dead_letter = self.dlt(source)
        row = expect_snapshot(following)
        require(snapshot(event["courseId"]) is None, "Invalid event wrote a snapshot")
        return {"source": source, "deadLetter": dead_letter, "followingSource": next_source,
                "followingSnapshot": row, "committedAfterRecovery": self.consumed(next_source)}

    def case_four(self):
        retry_budget("5s")
        self.barrier()
        since = now()
        event = self.event("Retry budget exceeded")
        compose("stop", "-t", "5", DB)
        started = time.monotonic()
        try:
            source = self.send(event)
            dead_letter = self.dlt(source, timeout=90)
            elapsed = round(time.monotonic() - started, 1)
            failures = self.database_failures(since)
            self.evidence["cases"][-1]["evidence"] = {
                "source": source, "deadLetter": dead_letter, "databaseFailureCount": failures,
                "elapsedSeconds": elapsed}
            require(elapsed >= 5 and failures >= 2, "Event did not exhaust retry budget through real DB failures")
            committed = self.consumed(source)
        finally:
            db_start()
        require(snapshot(event["courseId"]) is None, "DLT event unexpectedly wrote a snapshot")
        following = self.event("Database available again")
        next_source = self.send(following, partition=source["partition"])
        row = expect_snapshot(following)
        self.consumed(next_source)
        retry_budget("5m")
        self.barrier()
        return {"retryBudget": "5s", "elapsedSeconds": elapsed, "databaseFailureCount": failures,
                "source": source, "deadLetter": dead_letter, "committedAfterDlt": committed,
                "followingSource": next_source, "followingSnapshot": row, "restoredRetryBudget": "5m"}

    def deny_dlt_writes(self):
        self.acls_changed = True
        for future in self.admin.create_acls(self.dlt_acls).values():
            future.result(15)
        probe = Producer({"bootstrap.servers": "localhost:9092", "acks": "all",
                          "delivery.timeout.ms": 5000})

        def denied():
            delivered = []
            probe.produce(DLT, key="qa-dlt-write-probe", value=self.run_id,
                          on_delivery=lambda error, message: delivered.append(error))
            require(probe.flush(6) == 0 and bool(delivered), "DLT fault probe did not complete")
            error = delivered[0]
            require(error is None or error.code() == KafkaError.TOPIC_AUTHORIZATION_FAILED,
                    "DLT probe failed for a reason other than denied write permission")
            return error is not None

        # ACL metadata can reach the broker after the admin response. Prove the
        # failure is effective before evaluating application recovery behavior.
        wait_for("broker rejects DLT writes", denied, timeout=20)

    def case_five(self):
        self.barrier()
        since = now()
        self.deny_dlt_writes()
        source = self.send(raw="not-json-dlt-unavailable", key=self.event()["courseId"])
        following = self.event("After DLT recovery")
        next_source = self.send(following, partition=source["partition"])
        require(next_source["offset"] == source["offset"] + 1, "Following record is not adjacent")
        started = time.monotonic()
        observations = []
        self.evidence["cases"][-1]["evidence"] = {
            "source": source, "followingSource": next_source, "offsetsWhileDltUnavailable": observations,
            "faultProbe": "TOPIC_AUTHORIZATION_FAILED"}
        publication_failed_at = None
        try:
            # Require an actual failed recovery plus further observations, not
            # just an in-flight send. Source writes and DLT reads remain allowed.
            while time.monotonic() - started < 60:
                elapsed = round(time.monotonic() - started, 1)
                offset = self.committed(source["partition"])
                require(offset <= source["offset"], "Lost source offset before DLT acknowledged")
                require(snapshot(following["courseId"]) is None, "Consumer skipped failed DLT record")
                observations.append({"seconds": elapsed, "committed": offset})
                message = self.dlt_reader.poll(0.1)
                if message is not None:
                    require(message.error() is None, "DLT reader failed")
                    require(message.value() != source["payload"].encode(), "DLT unexpectedly writable")
                if re.search(r"Dead-letter publication.*failed|Dead-letter publication.*timed out",
                             self.logs(since), re.IGNORECASE):
                    publication_failed_at = publication_failed_at or elapsed
                if publication_failed_at is not None and elapsed - publication_failed_at >= 5:
                    break
                time.sleep(3)
            require(publication_failed_at is not None, "No actual failed DLT publication observed")
        finally:
            self.restore_dlt()
        dead_letter = self.dlt(source, timeout=150)
        row = expect_snapshot(following)
        return {"fault": "DENY WRITE ACL on DLT; source writes and DLT reads allowed",
                "faultProbe": "TOPIC_AUTHORIZATION_FAILED",
                "source": source, "offsetsWhileDltUnavailable": observations,
                "publicationFailedAtSeconds": publication_failed_at, "deadLetter": dead_letter,
                "followingSource": next_source, "followingSnapshot": row,
                "committedAfterRecovery": self.consumed(next_source), "dltConfigRestored": True}

    def run(self):
        self.prepare()
        try:
            for code, action in [("ENROLL-09.1", self.case_one),
                                 ("ENROLL-09.2", lambda: self.invalid_case(True)),
                                 ("ENROLL-09.3", lambda: self.invalid_case(False)),
                                 ("ENROLL-09.4", self.case_four), ("ENROLL-09.5", self.case_five)]:
                result = {"case": code, "startedAt": now(), "status": "FAIL"}
                self.evidence["cases"].append(result)
                print(f"Running {code}", flush=True)
                try:
                    result["evidence"] = action()
                    result["status"] = "PASS"
                    print(f"PASS {code}", flush=True)
                except AssertionError as error:
                    result["failure"] = str(error)
                    raise
                finally:
                    result["completedAt"] = now()
                    self.save()
        finally:
            # Attempt each cleanup independently, including after a failed assertion.
            for label, action in [("mysqlRestored", db_start),
                                  ("retryBudgetRestored", lambda: retry_budget("5m")),
                                  ("dltConfigRestored", self.restore_dlt)]:
                try:
                    action()
                    self.evidence["cleanup"][label] = True
                except Exception:
                    self.evidence["cleanup"][label] = False
            for reader in self.readers:
                reader.close()
            self.save()
        require(all(self.evidence["cleanup"].values()), "Test infrastructure cleanup failed")

    def restore_dlt(self):
        if self.acls_changed:
            filters = [AclBindingFilter(acl.restype, acl.name, acl.resource_pattern_type,
                                        acl.principal, acl.host, acl.operation, acl.permission_type)
                       for acl in self.dlt_acls]
            for future in self.admin.delete_acls(filters).values():
                future.result(15)
            self.acls_changed = False

    def save(self):
        OUTPUT.parent.mkdir(parents=True, exist_ok=True)
        self.evidence["updatedAt"] = now()
        OUTPUT.write_text(json.dumps(self.evidence, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    Probe().run()
