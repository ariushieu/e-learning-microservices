// Keep raw Newman reports (which contain credentials/tokens) out of CI artifacts and Git.
const fs = require("node:fs");
const crypto = require("node:crypto");
const { execFileSync } = require("node:child_process");

const report = JSON.parse(fs.readFileSync(process.argv[2], "utf8"));
const run = report.run;
const executions = run.executions.map((execution) => ({
  name: execution.item.name,
  http: execution.response?.code ?? null,
  assertions: (execution.assertions ?? []).length,
  failedAssertions: (execution.assertions ?? [])
    .filter((assertion) => assertion.error)
    .map((assertion) => assertion.assertion),
}));
const fixtureCases = {
  "AUTH-03.8": "Requires a genuinely expired refresh token",
  "AUTH-05.5": "Requires a genuinely expired access token",
  "AUTH-07.10": "Requires a valid access token issued before its account was deleted",
};
const blocked = Object.entries(fixtureCases)
  .filter(([id]) => !executions.some((execution) => execution.name.startsWith(id + " ")))
  .map(([id, reason]) => ({ id, reason }));
const result = {
  sourceCommit:
    process.env.SOURCE_COMMIT ??
    execFileSync("git", ["rev-parse", "HEAD"], { encoding: "utf8" }).trim(),
  collectionSha256: crypto
    .createHash("sha256")
    .update(fs.readFileSync("docs/postman/auth.postman_collection.json"))
    .digest("hex"),
  startedAt: new Date(run.timings.started).toISOString(),
  completedAt: new Date(run.timings.completed).toISOString(),
  stats: run.stats,
  blocked,
  executions,
  failures: run.failures.map((failure) => ({
    name: failure.source?.name,
    assertion: failure.error?.test ?? failure.error?.name,
  })),
};
fs.writeFileSync(process.argv[3], JSON.stringify(result, null, 2) + "\n");
console.log(
  JSON.stringify({ requests: run.stats.requests, assertions: run.stats.assertions, blocked }),
);
