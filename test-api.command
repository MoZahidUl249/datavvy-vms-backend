#!/bin/bash
# Manual API test (create → list → get → update → duplicate → delete). Start run-backend.command first.
cd "$(dirname "$0")" || exit 1
bash scripts/api-smoke.sh 2>&1 | tee api-test.log
echo; echo "Log saved to api-test.log — you can close this window."
