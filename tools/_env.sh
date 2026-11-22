# Shared by the *.command scripts: finds JDK 21 and Maven (downloads Maven locally if missing).
cd "$(dirname "$0")" || exit 1
ROOT="$PWD"
if [ -x /usr/libexec/java_home ] && /usr/libexec/java_home -v 21 >/dev/null 2>&1; then
  export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
  export PATH="$JAVA_HOME/bin:$PATH"
fi
JAVA_MAJOR=$(java -version 2>&1 | awk -F'"' '/version/ {split($2,a,"."); print a[1]}')
if [ -z "$JAVA_MAJOR" ] || [ "$JAVA_MAJOR" -lt 21 ]; then
  echo "!! JDK 21 or newer is required (found: ${JAVA_MAJOR:-none})."
  echo "   Install it with:  brew install openjdk@21   (or download Temurin 21 from adoptium.net)"
  exit 1
fi
if command -v mvn >/dev/null 2>&1; then
  MVN=mvn
else
  MVN_VERSION=3.9.11
  MVN_HOME="$ROOT/.tools/apache-maven-$MVN_VERSION"
  if [ ! -x "$MVN_HOME/bin/mvn" ]; then
    echo "== Maven not found; downloading Maven $MVN_VERSION into .tools/"
    mkdir -p "$ROOT/.tools"
    curl -fsSL "https://archive.apache.org/dist/maven/maven-3/$MVN_VERSION/binaries/apache-maven-$MVN_VERSION-bin.tar.gz" \
      | tar -xz -C "$ROOT/.tools" || { echo "!! Maven download failed"; exit 1; }
  fi
  MVN="$MVN_HOME/bin/mvn"
fi

# Local settings: credentials live in the git-ignored .env, never in the repo.
# First run: create .env from .env.example with fresh random secrets.
if [ ! -f "$ROOT/.env" ]; then
  rnd() { LC_ALL=C tr -dc 'A-Za-z0-9' < /dev/urandom | head -c "$1"; }
  sed -e "s|^VMS_DB_PASSWORD=.*|VMS_DB_PASSWORD='$(rnd 24)'|" \
      -e "s|^MYSQL_ROOT_PASSWORD=.*|MYSQL_ROOT_PASSWORD='$(rnd 24)'|" \
      -e "s|^VMS_JWT_SECRET=.*|VMS_JWT_SECRET='$(rnd 48)'|" \
      "$ROOT/.env.example" > "$ROOT/.env"
  chmod 600 "$ROOT/.env"
  echo "== Created .env with random local passwords (git-ignored)"
fi
set -a; . "$ROOT/.env"; set +a
