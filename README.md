# Codenames

Simple Codenames App for Collins Family

See my [blog post](http://www.cartesiantheatrics.com/2020/04/14/codenames-app.html) for discussion of the motivation and ideas behind the project.

# Build

Make sure you have the clojure CLI tool installed.

```bash
make build
```

# Run

```
java -cp "target/codenames-0.0.1-SNAPSHOT.jar:target/lib/lib/*" codenames.core
```

Hosted by default at localhost:3001.

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
