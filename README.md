# redis-s-expression

The Redis datastore only allows string values.

Using the redis-clojure library, this allows homoiconic persistence on top of 
the redis store. Aims to treat data as native as possible for redis while 
leveraging s-expressions on the values of each atomic element in the redis 
datastore.

## Usage

You will need leiningen. There isn't anything on Clojars for this, since the 
project is so young and things will change wildly for now. So you should use
git-deps for now. Here is a starting point for your project.clj:

	(defproject your-project "0.1.0-SNAPSHOT"
	  :description "Your Project"
	  :dependencies [[org.clojure/clojure "1.4.0"] 
	  				 [org.clojars.tavisrudd/redis-clojure "1.3.0"]]
	  :dev-dependencies [[lein-git-deps "0.0.1-SNAPSHOT"]]
	  :git-dependencies [["git@github.com:nickbauman/redis-s-expression.git"]]
	  :extra-classpath-dirs [".lein-git-deps/redis-s-expression.git/src"]
	  :main your.project.main)

So keep in mind to do:

	lein deps, git-deps

## License

Copyright (C) 2012 Nick Bauman

Distributed under the Eclipse Public License, the same as Clojure.
