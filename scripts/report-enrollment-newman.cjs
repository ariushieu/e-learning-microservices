// Upload only this allowlisted summary, never the raw report or exported collection.
const fs = require("node:fs");
const crypto = require("node:crypto");
const run = JSON.parse(fs.readFileSync(process.argv[2], "utf8")).run;
const result = {
  sourceCommit: process.env.SOURCE_COMMIT,
  collectionSha256: crypto.createHash("sha256")
    .update(fs.readFileSync("docs/postman/enrollment.postman_collection.json")).digest("hex"),
  startedAt: new Date(run.timings.started).toISOString(),
  completedAt: new Date(run.timings.completed).toISOString(),
  stats: run.stats,
  manualCasesNotExecuted: ["ENROLL-09.1", "ENROLL-09.2", "ENROLL-09.3", "ENROLL-09.4", "ENROLL-09.5",
    "ENROLL-13.6", "ENROLL-13.7"],
  executions: run.executions.map((execution) => ({
    name: execution.item.name,
    http: execution.response?.code ?? null,
    assertions: (execution.assertions ?? []).length,
    failedAssertions: (execution.assertions ?? []).filter((assertion) => assertion.error)
      .map((assertion) => assertion.assertion),
  })),
  failures: run.failures.map((failure) => ({ name: failure.source?.name, assertion: failure.error?.test ?? failure.error?.name })),
};
fs.writeFileSync(process.argv[3], JSON.stringify(result, null, 2) + "\n");
console.log(JSON.stringify({ requests: run.stats.requests, assertions: run.stats.assertions }));
