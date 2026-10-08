"""Exercise V5 on disposable MySQL schemas (never uses an application database).

MYSQL_BIN defaults to mysql; MYSQL_HOST/PORT/USER select the test server.
Use the client's option file or MYSQL_PWD for credentials, never command arguments.
"""
import json
import os
from pathlib import Path
import subprocess
import uuid

root = Path(__file__).resolve().parent.parent
base = [os.environ.get("MYSQL_BIN", "mysql"), "--default-character-set=utf8mb4",
        "--host=" + os.environ.get("MYSQL_HOST", "127.0.0.1"),
        "--port=" + os.environ.get("MYSQL_PORT", "3306"),
        "--user=" + os.environ.get("MYSQL_USER", "root"), "--batch", "--skip-column-names"]
flags = subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0
migrations = root / "auth-service/src/main/resources/db/migration"
v1 = (migrations / "V1__init_auth_schema.sql").read_text(encoding="utf-8")
v5 = (migrations / "V5__normalize_email_and_use_binary_collation.sql").read_text(encoding="utf-8")
results = []


def sql(statement, success=True):
    result = subprocess.run(base, input=statement, encoding="utf-8", capture_output=True,
                            creationflags=flags, timeout=30)
    if success:
        assert result.returncode == 0, result.stderr
    return result


for scenario in ["empty", "existing", "collision"]:
    schema = "qa_email_" + uuid.uuid4().hex
    try:
        sql(f"CREATE DATABASE {schema}; USE {schema};\n" + v1)
        if scenario == "existing":
            sql(f"USE {schema}; INSERT INTO users(email,password_hash,full_name) VALUES "
                "('  HOCVIEN@EXAMPLE.COM  ','hash','Existing'),"
                "('HỌCVIÊN@EXAMPLE.COM','hash','Legacy Unicode');")
        if scenario == "collision":
            sql(f"USE {schema}; INSERT INTO users(email,password_hash,full_name) VALUES "
                "('HOCVIEN@EXAMPLE.COM','hash','First'),"
                "(' hocvien@example.com','hash','Second');")
        before = sql(f"SELECT id,HEX(email),password_hash FROM {schema}.users ORDER BY id;").stdout
        result = sql(f"USE {schema};\n" + v5, success=scenario != "collision")
        collation = sql("SELECT COLLATION_NAME FROM information_schema.columns "
                        f"WHERE TABLE_SCHEMA='{schema}' AND TABLE_NAME='users' AND COLUMN_NAME='email';").stdout.strip()
        if scenario == "collision":
            assert result.returncode != 0 and "uk_normalized_email_collision" in result.stderr
            assert before == sql(f"SELECT id,HEX(email),password_hash FROM {schema}.users ORDER BY id;").stdout
            assert collation == "utf8mb4_unicode_ci"
        else:
            assert collation == "utf8mb4_bin"
        if scenario == "existing":
            assert sql(f"SELECT email FROM {schema}.users ORDER BY id;").stdout.splitlines() == [
                "hocvien@example.com", "họcviên@example.com"]
            assert sql(f"SELECT COUNT(*) FROM {schema}.users WHERE email='hocviên@example.com';").stdout.strip() == "0"
            assert sql(f"SELECT COUNT(*) FROM {schema}.users WHERE LOWER(email) LIKE '%hocviên@example.com%';").stdout.strip() == "0"
            assert sql(f"SELECT COUNT(*) FROM {schema}.users WHERE email='hocvien@example.com';").stdout.strip() == "1"
            assert sql(f"SELECT COUNT(*) FROM {schema}.users WHERE password_hash='hash';").stdout.strip() == "2"
        results.append({"scenario": scenario, "result": "PASS", "collation": collation})
    finally:
        sql(f"DROP DATABASE IF EXISTS {schema};")

print(json.dumps({"mysql": sql("SELECT VERSION();").stdout.strip(), "results": results}, indent=2))
