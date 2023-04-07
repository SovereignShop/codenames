# Codenames

Simple Codenames App for Collins Family

# Build

Make sure you have the clojure CLI tool installed.

```bash
make build
```

# Run

```
java -cp "target/codenames-0.0.1-SNAPSHOT.jar:target/lib/lib/*" codenames.core
```


# Development

Startup nrepl servers:

```bash
# Clojure
clj -A:clj-prod:clj-dev

# Clojurescript
clj -A:cljs-prod:cljs-dev  # Preloaded with figwheel.main
```

Connect to them using your favorite editor. You can launch figwheel main 
after connecting.

## Prerequisites.

1. Learn datalog: http://www.learndatalogtoday.org
2. Learn datascript: https://github.com/tonsky/datascript 
3. Learn re-frame: https://github.com/day8/re-frame
