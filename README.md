# cljml-core-matrix

A Clojure library designed to convert datasets in [Clj-ml](https://github.com/joshuaeckroth/clj-ml) (Weka) format to [core.matrix](https://github.com/mikera/core.matrix) format, and vice versa.


## Rationale

Core.matrix is an excellent Clojure library for manipulation and transformation of arrays, datasets and matrices.  It is also the library backing the Incanter toolkit. 

To run machine learning algorithms such as classification or regression, one needs to use other libraries such as [Clj-ml](https://github.com/joshuaeckroth/clj-ml), which is a wrapper on the Weka toolkit implemented in Java.

However, the dataset formats used by Core.matrix and Clj-ml are different. This library enables one to load a dataset, do  transformation and data cleaning tasks and then run machine learning algorithms in clj-ml.

## Usage

Create a core.matrix dataset and convert it to Cljml format:

``` clojure

(ns your-ns 
    (:require [clojure.core.matrix :as cm]
              [clojure.core.matrix.dataset :as cd]
	      [cljml-core-matrix.core :as ccm :refer [to-cljml-dataset]]))

(-> (cd/dataset [:a :b] [[1 2] [3 4]])
    ;clj-ml datasets also needs a name associated with the dataset
    (to-cljml-dataset "test"))

```


## License

Copyright © 2017 Kiran Karkera

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
