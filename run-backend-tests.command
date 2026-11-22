#!/bin/bash
# Runs the whole backend test suite (unit + integration + architecture). Double-click in Finder.
# Log: backend-tests.log
source "$(dirname "$0")/tools/_env.sh" || { read -n1 -p "Press any key to close"; exit 1; }
LOG="$ROOT/backend-tests.log"
{
  echo "== $(date)"; java -version 2>&1 | head -1; "$MVN" -v | head -1
  echo "== mvn verify"
  "$MVN" -B -f pom.xml verify
  STATUS=$?
  echo
  echo "== Test summary"
  for f in */target/surefire-reports/*.txt; do [ -f "$f" ] && sed -n '4p' "$f"; done
  [ $STATUS -eq 0 ] && echo "== ALL TESTS PASSED" || echo "== BUILD/TESTS FAILED (exit $STATUS)"
} 2>&1 | tee "$LOG"
echo; echo "Log saved to backend-tests.log — you can close this window."
