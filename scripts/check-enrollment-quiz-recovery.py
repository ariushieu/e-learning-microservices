"""ENROLL-13.6/13.7: real quiz outbox, dedicated MySQL outage and exact event replay.

Run only in the enrollment-resilience Compose project after demo-flow/ENROLL-09.
Reuse its safety checks; never export login responses, tokens or raw service logs.
"""
import importlib.util
import json
import os
from pathlib import Path
import time
import uuid

from confluent_kafka import ConsumerGroupTopicPartitions, TopicPartition

spec = importlib.util.spec_from_file_location("recovery", Path(__file__).with_name("check-enrollment-resilience.py"))
recovery = importlib.util.module_from_spec(spec)
spec.loader.exec_module(recovery)
TOPIC = "elearning.quiz.events"
GROUP = "enrollment-quiz-progress"
output = Path("target/enrollment-resilience/quiz-recovery.json")


def run():
    probe = recovery.Probe()
    evidence = {"sourceCommit": os.environ.get("SOURCE_COMMIT"), "startedAt": recovery.now(), "cases": []}
    outage = False
    try:
        probe.prepare()  # Check project, container labels and dedicated DB_HOST before any mutation.
        reader = probe.reader(TOPIC)
        suffix = uuid.uuid4().hex
        admin = recovery.api("POST", "/api/auth/login", {"email": "admin@elearning.hunre.edu.vn", "password": "Admin@123456"})["accessToken"]
        category = recovery.api("POST", "/api/categories", {"name": "Quiz recovery " + suffix, "slug": "quiz-recovery-" + suffix}, admin, 201)
        course = recovery.api("POST", "/api/courses", {"categoryId": category["id"], "title": "Quiz recovery " + suffix,
                              "slug": "quiz-recovery-" + suffix, "price": 0, "level": "BEGINNER", "language": "vi"}, admin, 201)
        course_id = course["id"]
        section = recovery.api("POST", f"/api/courses/{course_id}/sections", {"title": "Recovery", "position": 0}, admin, 201)
        lessons = [recovery.api("POST", f'/api/sections/{section["id"]}/lessons',
                   {"title": f"Lesson {n}", "type": "ARTICLE", "content": "QA", "position": n}, admin, 201) for n in (1, 2)]
        recovery.api("PATCH", f"/api/courses/{course_id}/status", {"status": "PUBLISHED"}, admin)
        recovery.wait_for("curriculum snapshot", lambda: recovery.sql(
            f"SELECT JSON_CONTAINS(lesson_ids, '{lessons[1]['id']}') FROM course_snapshots WHERE course_id = {course_id}") == "1")
        email = "quiz-recovery-" + suffix + "@example.com"
        recovery.api("POST", "/api/auth/register", {"email": email, "password": "Test@123456", "fullName": "Quiz recovery learner"}, expected=201)
        learner = recovery.api("POST", "/api/auth/login", {"email": email, "password": "Test@123456"})
        token = learner["accessToken"]
        enrollment = recovery.api("POST", "/api/enrollments", {"courseId": course_id}, token, 201)
        enrollment_id = enrollment["id"]
        recovery.api("PUT", f'/api/lessons/{lessons[0]["id"]}/progress', {"courseId": course_id, "status": "COMPLETED"}, token)
        quiz = recovery.api("POST", "/api/quizzes", {"courseId": course_id, "lessonId": lessons[1]["id"],
                            "title": "Recovery quiz", "passScore": 50, "maxAttempts": 0}, admin, 201)
        question = recovery.api("POST", f'/api/quizzes/{quiz["id"]}/questions', {"content": "2 + 2?", "type": "SINGLE_CHOICE", "score": 1,
                                "options": [{"content": "4", "isCorrect": True}, {"content": "5", "isCorrect": False}]}, admin, 201)
        recovery.api("PATCH", f'/api/quizzes/{quiz["id"]}/status', {"status": "PUBLISHED"}, admin)
        attempt = recovery.api("POST", f'/api/quizzes/{quiz["id"]}/attempts', token=token, expected=201)
        correct = next(option["id"] for option in question["options"] if option["isCorrect"])

        def committed(partition):
            request = ConsumerGroupTopicPartitions(GROUP, [TopicPartition(TOPIC, partition)])
            return probe.admin.list_consumer_group_offsets([request])[GROUP].result(15).topic_partitions[0].offset

        since = recovery.now()
        recovery.compose("stop", "-t", "5", recovery.DB)
        outage = True
        result = recovery.api("POST", f'/api/attempts/{attempt["id"]}/submit',
                              {"answers": [{"questionId": question["id"], "selectedOptionIds": [correct]}]}, token)
        recovery.require(result["passed"], "Quiz did not pass")

        def source_event():
            message = reader.poll(1)
            if message is None:
                return None
            recovery.require(message.error() is None, "Quiz source observer failed")
            return message if json.loads(message.value()).get("attemptId") == attempt["id"] else None

        source = recovery.wait_for("real quiz outbox delivery while enrollment DB offline", source_event, timeout=45)
        event = json.loads(source.value())
        recovery.require(event["lessonId"] == lessons[1]["id"], "Quiz event lost lessonId")
        observations = []
        deadline = time.monotonic() + 12
        while time.monotonic() < deadline:
            offset = committed(source.partition())
            recovery.require(offset <= source.offset(), "Quiz offset advanced during MySQL outage")
            observations.append(offset)
            time.sleep(2)
        # Spring Boot abbreviates thread names to 15 characters in its default log pattern.
        failure_lines = sum("progress-0-C-1" in line and "Connection is not available" in line
                            for line in probe.logs(since).splitlines())
        recovery.require(failure_lines > 0, "No quiz consumer database failure observed")
        recovery.db_start()
        outage = False
        recovery.wait_for("quiz committed after recovery", lambda: committed(source.partition()) > source.offset(), timeout=100)

        def state():
            return json.loads(recovery.sql(
                "SELECT JSON_OBJECT('status', e.status, 'progress', e.progress_percent, "
                "'lessonCount', (SELECT COUNT(*) FROM lesson_progress p WHERE p.enrollment_id=e.id AND p.status='COMPLETED'), "
                "'certificateCount', (SELECT COUNT(*) FROM certificates c WHERE c.enrollment_id=e.id), "
                "'completedEvents', (SELECT COUNT(*) FROM outbox_events o WHERE o.event_type='enrollment.completed' AND o.aggregate_id=CAST(e.id AS CHAR)), "
                "'certificateEvents', (SELECT COUNT(*) FROM outbox_events o WHERE o.event_type='certificate.issued' AND JSON_EXTRACT(o.payload, '$.enrollmentId')=e.id), "
                f"'ledgerCount', (SELECT COUNT(*) FROM processed_quiz_events p WHERE p.event_id='{str(uuid.UUID(event['eventId']))}')) "
                f"FROM enrollments e WHERE e.id={enrollment_id}"))

        before = state()
        expected = {"status": "COMPLETED", "progress": 100, "lessonCount": 2, "certificateCount": 1,
                    "completedEvents": 1, "certificateEvents": 1, "ledgerCount": 1}
        recovery.require(before == expected, "Recovery did not commit exactly one complete progress transaction")
        evidence["cases"].append({"case": "ENROLL-13.7", "status": "PASS", "offsetsDuringOutage": observations,
                                  "databaseFailureLogLines": failure_lines,
                                  "source": {"topic": TOPIC, "partition": source.partition(), "offset": source.offset(), "eventId": event["eventId"]}, "state": before})

        # Replay identical bytes/key, including eventId; no new submission or newly generated event.
        deliveries = []
        for _ in range(3):
            probe.producer.produce(TOPIC, key=source.key(), value=source.value(), partition=source.partition(),
                                   on_delivery=lambda error, message: deliveries.append((error, message.offset())))
        recovery.require(probe.producer.flush(20) == 0 and len(deliveries) == 3 and all(error is None for error, _ in deliveries),
                         "Replay delivery failed")
        last = max(offset for _, offset in deliveries)
        recovery.wait_for("replayed events acknowledged", lambda: committed(source.partition()) > last)
        after = state()
        recovery.require(after == before, "Duplicate delivery changed progress/certificate/outbox")
        evidence["cases"].append({"case": "ENROLL-13.6", "status": "PASS", "replayedCopies": 3,
                                  "lastReplayOffset": last, "committedOffset": committed(source.partition()), "state": after})
    except Exception as error:
        evidence["failure"] = str(error)
        raise
    finally:
        if outage:
            recovery.db_start()
        for reader in probe.readers:
            reader.close()
        evidence["completedAt"] = recovery.now()
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(json.dumps(evidence, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    run()
