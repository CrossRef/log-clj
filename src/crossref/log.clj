(ns crossref.log
  (:require [taoensso.timbre :as timbre]
            [clojure.data.json :as json]
            [clj-time.format :as time.format]
            [clj-time.coerce :as time.coerce]))

(def iso-formatter (time.format/formatter "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))

(defn write-json-date [in ^java.io.PrintWriter out]
  (.print out (str "\"" (time.format/unparse iso-formatter in) "\"")))

(extend org.joda.time.DateTime json/JSONWriter {:-write write-json-date})

(extend java.util.Date json/JSONWriter {:-write #(write-json-date (time.coerce/from-date %1) %2)})

(defn data->json [data extras]
  (json/write-str
   (merge extras
          (:context data)
          {:level (:level data)
           :namespace (:?ns-str data)
           :file (:?file data)
           :line (:?line data)
           :stacktrace (some-> (:?err data) timbre/stacktrace)
           :hostname (force (:hostname_ data))
           :message (force (:msg_ data))
           "@timestamp" (:instant data)})))

(defn prepare-logging
  "Prepare logging with some flags. Recommend :app-name, :component-name
   and :component-version."
  [extras]
  (timbre/set-config!
   {:level :trace
    :output-fn (fn [data] (data->json data extras))
    :appenders {:standard-out (timbre/println-appender {:stream :auto})}}))
                           
  
