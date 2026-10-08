// Allowlist only: raw Newman reports contain login responses and must stay temporary.
const fs = require("node:fs");
const crypto = require("node:crypto");
const run = JSON.parse(fs.readFileSync(process.argv[2], "utf8")).run;
const result = {
  sourceCommit: process.env.SOURCE_COMMIT,
  collectionSha256: crypto.createHash("sha256")
    .update(fs.readFileSync("docs/postman/demo-flow.postman_collection.json")).digest("hex"),
  startedAt: new Date(run.timings.started).toISOString(),
  completedAt: new Date(run.timings.completed).toISOString(),
  requests: run.stats.requests,
  assertions: run.stats.assertions,
  executions: run.executions.map((entry) => ({
    name: entry.item.name,
    http: entry.response?.code ?? null,
    assertions: (entry.assertions ?? []).length,
    failedAssertions: (entry.assertions ?? []).filter((a) => a.error).map((a) => a.assertion),
  })),
  failures: run.failures.map((f) => ({ name: f.source?.name, assertion: f.error?.test ?? f.error?.name })),
};
fs.writeFileSync(process.argv[3], JSON.stringify(result, null, 2) + "\n");
console.log(JSON.stringify({ requests: result.requests, assertions: result.assertions }));
