(ns cljml-core-matrix.core
  "Convert core.matrix datasets to weka datasets and vice versa, load/save arff files and other utilities"
  (:require [clojure.core.matrix :as cm]
            [clojure.core.matrix.dataset :as cd]
            [clojure.core.matrix.impl.dataset :as cmimpl]
            [clojure.test :as t :refer [with-test is]]
            [com.rpl.specter :as sp :refer (select transform)]
            [clojure.java.io :as jio]
            [clj-ml.data :as cmd]
            [clj-ml.io :as cmio])
  (:import [weka.core Instances]))

(with-test
  (defn get-column-descr
    "Returns a vector of column names,
    if a column has nominal values, its entry is a map where key is the column name and 
    value is a vector of the nominal values.
    
    Clj-ml datasets require the column type to be specified in advance. Columns with categorical values
    need to have all the values specified in a vector"
    ([dset] (get-column-descr dset #{}))
    ([dset colset]
     (let [kmap (cd/to-map dset)
           cfn (fn [k]
                 (let [v (kmap k)]
                   (if (every? #(or (number? %) (nil? %)) v) k {k (vec (if (empty? colset)
                                                                         (set v) colset))})))]
       (mapv cfn (cd/column-names dset)))))
  (is (= [:a {:b ["a" "b"]}]
         (get-column-descr (cd/dataset [:a :b] [[1 "a"] [3 "b"]]))))
  (is (= [:a {:b ["ab" "a" "b"]}]
         (get-column-descr (cd/dataset [:a :b] [[1 "a"] [3 "b"]]) #{"a" "ab" "b"})))
  (is (= [:a :b]
         (get-column-descr (cd/dataset [:a :b] [[1 2] [3 nil]])))))

(with-test
  (defn type-coerce
    "cljml datasets need all values in a column to have the same type. NA values can be represented as nil, not empty strings. 
  This method returns the dataset and replaces strings with nils"
    ([dset] (type-coerce dset (fn [x] (if (string? x) nil x))))
    ([dset string-replace-fn]
     (let [m (cd/to-map dset)
           clfreq (transform [sp/MAP-VALS] (fn [v] (frequencies (map type v))) m)
        ;;list of columns that need to be transformed
           repl-colnames (->> clfreq
                              (filter (fn [[k v]]
                                        (let [res (-> v keys set)]
                                          (and (res java.lang.String)
                                               (or (res java.lang.Double) (res java.lang.Long))))))
                              (mapv first))
           replaced-columns (->> repl-colnames
                                 (mapv #(cd/column dset %))
                                 (mapv #(mapv string-replace-fn %)))]
       (reduce (fn [dse [cname cdata]] (cd/replace-column dse cname cdata))
               dset (mapv vector repl-colnames replaced-columns)))))
  (is (= (cd/dataset [:a :b] [[nil 2] [2.0 4]])
         (type-coerce (cd/dataset [:a :b] [["" 2] [2.0 4]]))))
  (is (= (cd/dataset [:a :b] [[nil 2] [2 4]])
         (type-coerce (cd/dataset [:a :b] [["" 2] [2 4]])))))

(with-test
  (defn to-cljml-dataset
    "Returns a clj-ml dataset given a core.matrix dataset and dataset name
  Optional argument is the column description for each column "
    ([dset dset-name]
     (let [w1 (type-coerce dset)]
       (cmd/make-dataset dset-name
                         (get-column-descr w1)
                         (cm/to-nested-vectors w1))))
    ([dset dset-name col-descr]
     (cmd/make-dataset dset-name
                       col-descr
                       (cm/to-nested-vectors dset))))
  (let [wdset (to-cljml-dataset (cd/dataset [:a :b] [[1 2] [3 4]]) "test")]
    ;verify type
    (is (= cljml.ClojureInstances (class wdset)))
    ;verify column names
    (is (every? #{:a :b} (cmd/attribute-names wdset)))
    ;verify number of rows
    (is (= 2 (cmd/dataset-count wdset)))))

(with-test
  (defn to-corematrix-dataset
    "Convert a cljml dataset to a core.matrix dataset"
    [^Instances dset]
    (let [colnames (cmd/attribute-names dset)]
      (cd/dataset colnames (mapv cmd/instance-to-vector (cmd/dataset-seq dset)))))
  (let [dset (to-corematrix-dataset (cmd/make-dataset "test"
                                                      [:a :b]
                                                      [[1 2] [3 4]]))]
    (is (true? (cd/dataset? dset)))
    ;verify column names
    (is (every? #{:a :b} (cd/column-names dset)))
    ;verify shape
    (is (= [2 2] (cm/shape dset)))))

(with-test
  (defn load-corematrix-dataset
    "Returns a core.matrix dataset read from file. The file formats supported are :arff, :csv, :xrff and :libsvm"
    [file-format filepath]
    {:pre [(is (#{:arff :xrff :libsvm :csv} file-format)
               "Formats supported are :arff, :csv, :xrff and :libsvm")]}
    ;Using test/is for adding a helpful message, 
    ;hat tip to https://groups.google.com/d/msg/clojure/xHrFyDcPS9A/gXiNY6pmAwAJ
    (->> filepath
         (cmio/load-instances file-format)
         to-corematrix-dataset))
  (let [dset (load-corematrix-dataset :arff "http://storm.cis.fordham.edu/~gweiss/data-mining/weka-data/iris.arff")]
    (is (true? (cd/dataset? dset)))
    (is (= [150 5] (cm/shape dset)))))

(with-test
  (defn save-corematrix-dataset
    "Saves the core.matrix dataset. The file formats supported are :arff, :csv, :xrff and :libsvm"
    ( [file-format dset filepath] (save-corematrix-dataset file-format dset filepath "dataset-name"))
    ([file-format dset filepath dataset-name]
    {:pre [(is (#{:arff :xrff :libsvm :csv} file-format)
               "Formats supported are :arff, :csv, :xrff and :libsvm")]}
    (->> (to-cljml-dataset dset dataset-name)
        (cmio/save-instances file-format filepath))))
  (let [_ (save-corematrix-dataset :csv 
                                   (cd/dataset [:a :b] [[1 2] [3 4]])
                                   "test.csv")
        res (mapv #(vec (.split % ",")) (.split  (slurp "test.csv") "\n"))
        _ (jio/delete-file "test.csv")
        ]
   (is (= [["a" "b"] ["1" "2"] ["3" "4"]]) res)))
