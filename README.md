# clostack

openstack client for clojure

[![Clojars Project](https://img.shields.io/clojars/v/zf/clostack.svg)](https://clojars.org/zf/clostack)

## Usage

```clj
(require '[clostack.keystone :refer [token-create]])
(require '[clostack.nova :as nova])

(let [scope {:project {:name "admin" :domain {:name "Default"}}}
      token (token-create "http://keystone:5000/v3/auth/tokens" :name "admin" :password "secret" :scope scope)]
  (nova/server-list token {:all_tenants true}))
```

## License

```
The MIT License (MIT)

Copyright (c) 2015 Feng Zhou

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
