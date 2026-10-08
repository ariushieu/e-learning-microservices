"""Real gateway/MySQL check. Use a disposable QA database; creates one QA account."""
from concurrent.futures import ThreadPoolExecutor
import json
import os
from pathlib import Path
import urllib.error
import urllib.request
import uuid

base = os.environ.get("AUTH_GATEWAY_URL", "http://localhost:8080")
output = Path(os.environ.get("AUTH_CONCURRENCY_OUTPUT", "target/auth-login-events-concurrency.json"))
email = "qa.activity.concurrent." + uuid.uuid4().hex + "@example.com"
password = "ConcurrentActivity@123456"


def call(path, body=None, token=None):
    headers = {"Content-Type": "application/json", "User-Agent": "PostmanRuntime/7"}
    if token:
        headers["Authorization"] = "Bearer " + token
    request = urllib.request.Request(base + path, headers=headers,
                                     data=None if body is None else json.dumps(body).encode())
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return response.status, json.load(response)
    except urllib.error.HTTPError as error:
        return error.code, json.load(error)


assert call("/api/auth/register", {"email": email, "password": password, "fullName": "QA Concurrent"})[0] == 201
with ThreadPoolExecutor(max_workers=8) as pool:
    responses = list(pool.map(lambda _: call("/api/auth/login", {"email": email, "password": "Incorrect@123456"}), range(8)))
assert all(status == 401 for status, _ in responses), [status for status, _ in responses]
status, response = call("/api/auth/login", {"email": email, "password": password})
assert status == 200
token = response["data"]["accessToken"]
status, me = call("/api/auth/me", token=token)
assert status == 200 and me["data"]["failedLoginsSinceLastSuccess"] == 8
status, history = call("/api/auth/login-events", token=token)
assert status == 200 and history["data"]["totalElements"] == 9
assert [row["success"] for row in history["data"]["content"]] == [True] + [False] * 8
output.parent.mkdir(parents=True, exist_ok=True)
output.write_text(json.dumps({"result": "PASS", "concurrentFailures": 8, "failedHttp": [status for status, _ in responses],
                              "loginHttp": 200, "meHttp": 200, "warningCount": 8, "historyCount": 9}, indent=2) + "\n")
print("PASS: eight concurrent 401 attempts, warning=8, history=9 through gateway")
