(ns re-com.simple-v-table
  (:require-macros
    [re-com.core     :refer [handler-fn]]
    [re-com.validate :refer [validate-args-macro]])
  (:require
    [reagent.core    :as reagent]
    [re-com.config   :refer [include-args-desc?]]
    [re-com.box      :as box]
    [re-com.util     :refer [px deref-or-value assoc-in-if-empty]]
    [re-com.validate :refer [vector-of-maps? vector-atom? parts?]]
    [re-com.v-table  :as v-table]))

(defn swap!-sort-by-column
  [{:keys [key-fn order]} new-key-fn new-comp]
  (if (= key-fn new-key-fn)
    (if (= :asc order)
      {:key-fn key-fn
       :comp   new-comp
       :order  :desc}
      nil)
    {:key-fn new-key-fn
     :comp   new-comp
     :order :asc}))

(defn arrow-down
  []
  [:svg {:height "24" :viewBox "0 0 24 24" :width "24"}
   [:path {:d "M7 10l5 5 5-5H7z"}]])

(defn arrow-up
  []
  [:svg {:height "24" :viewBox "0 0 24 24" :width "24"}
   [:path {:d "M7 14l5-5 5 5H7z"}]])

(defn column-header
  [{:keys [id row-label-fn width height align vertical-align header-label sort-by] :as column} parts sort-by-column]
  (let [{:keys [key-fn comp] :or {key-fn row-label-fn comp compare}} sort-by
        {current-key-fn :key-fn order :order} @sort-by-column]
    [:<>
     [:div
      (merge
        {:class (str "rc-simple-v-table-column-header " (get-in parts [:simple-column-header :class]))
         :style (merge {:display        "inline-block"
                        :padding        (str "0px " "12px")
                        :width          (px (if sort-by (- width 24) width))
                        :min-height     (px 24)
                        :height         (px height)
                        :font-weight    "bold"
                        :text-align     align
                        :vertical-align vertical-align
                        :white-space    "nowrap"
                        :overflow       "hidden"
                        :text-overflow  "ellipsis"}
                       (when sort-by
                         {:cursor "pointer"})
                       (get-in parts [:simple-column-header :style]))}
        (when sort-by
          {:on-click #(swap! sort-by-column swap!-sort-by-column key-fn comp)})
        (get-in parts [:simple-column-header :attr]))
      header-label]
     (when sort-by
       [:div
        {:style {:display        "inline-block"
                 :width          (px 24)
                 :height         (px 24)
                 :text-align     align
                 :vertical-align vertical-align}}
        (when (= current-key-fn key-fn)
          (if (= order :desc)
            [arrow-down]
            [arrow-up]))])]))




(defn column-headers
  "Render the table header"
  [columns parts sort-by-column]
  (into
    [:div
     (merge
       {:class    (str "rc-simple-v-table-column-headers " (get-in parts [:simple-column-headers :class]))
        :style    (merge {:padding     "4px 0px"
                          :overflow    "hidden"
                          :white-space "nowrap"}
                         (get-in parts [:simple-column-headers :style]))
        :on-click (handler-fn (v-table/show-row-data-on-alt-click columns 0 event))}
       (get-in parts [:simple-column-headers :attr]))]
    (for [column columns]
      [column-header column parts sort-by-column])))

(defn row-column
  [row {:keys [width height align vertical-align row-label-fn] :as column} cell-style]
  [:div {:style (merge {:display        "inline-block"
                        :padding        (str "0px " "12px")
                        :width          (px width)
                        :height         (px height)
                        :text-align     align
                        :vertical-align vertical-align
                        :white-space    "nowrap"
                        :overflow       "hidden"
                        :text-overflow  "ellipsis"}
                       (if (fn? cell-style)
                         (cell-style row column)
                         cell-style))}
   (row-label-fn row)])

(defn row-columns
  "Render a single row of the table data"
  [columns on-click-row on-enter-row on-leave-row row-height row-style cell-style table-row-line-color row-index row]
  (into
    [:div
     {:class          (str "rc-simple-v-table-row ")
      :style          (merge {:padding     "4px 0px"
                              :overflow    "hidden"
                              :white-space "nowrap"
                              :height      (px row-height)
                              :border-top  (str "1px solid " table-row-line-color)
                              :cursor      (when on-click-row "pointer")}
                             (if (fn? row-style)
                               (row-style row)
                               row-style))
      :on-click       (handler-fn (do (v-table/show-row-data-on-alt-click row row-index event)
                                      (when on-click-row (on-click-row row-index))))
      :on-mouse-enter (when on-enter-row (handler-fn (on-enter-row row-index)))
      :on-mouse-leave (when on-leave-row (handler-fn (on-leave-row row-index)))}]
    (for [column columns]
      [row-column row column cell-style])))


(def simple-v-table-exclusive-parts-desc
  (when include-args-desc?
    [{:name :simple-wrapper        :level 0 :class "rc-simple-v-table-wrapper"        :impl "[simple-v-table]" :notes "Outer wrapper of the simple-v-table."}
     {:name :simple-column-headers :level 3 :class "rc-simple-v-table-column-headers" :impl "[:div]"}
     {:name :simple-column-header  :level 4 :class "rc-simple-v-table-column-header"  :impl "[:div]"}]))

(def simple-v-table-exclusive-parts
  (when include-args-desc?
    (-> (map :name simple-v-table-exclusive-parts-desc) set)))

(def simple-v-table-parts-desc
  (when include-args-desc?
    (into
      simple-v-table-exclusive-parts-desc
      (map #(update % :level inc) v-table/v-table-parts-desc))))

(def simple-v-table-parts
  (when include-args-desc?
    (-> (map :name simple-v-table-parts-desc) set)))

(def simple-v-table-args-desc
  (when include-args-desc?
    [{:name :model                     :required true                     :type "r/atom containing vec of maps" :validate-fn vector-atom?                   :description "one element for each row in the table."}
     {:name :columns                   :required true                     :type "vector of maps"                :validate-fn vector-of-maps?                :description [:span "one element for each column in the table. Must contain " [:code ":id"] "," [:code ":header-label"] "," [:code ":row-label-fn"] "," [:code ":width"] ", and " [:code ":height"] ". Optionally contains " [:code ":sort-by"] ", " [:code ":align"] " and " [:code ":vertical-align"] ". " [:code ":sort-by"] " can be " [:code "true"] " or a map optionally containing " [:code ":key-fn"] " and " [:code ":comp"] " ala " [:code "cljs.core/sort-by"] "."]}
     {:name :fixed-column-count        :required false :default 0         :type "integer"                       :validate-fn number?                        :description "the number of fixed (non-scrolling) columns on the left."}
     {:name :fixed-column-border-color :required false :default "#BBBEC0" :type "string"                        :validate-fn string?                        :description [:span "The CSS color of the horizontal border between the fixed columns on the left, and the other columns on the right. " [:code ":fixed-column-count"] " must be > 0 to be visible."]}
     {:name :column-header-height      :required false :default 31        :type "integer"                       :validate-fn number?                        :description [:span "px height of the column header section. Typically, equals " [:code ":row-height"] " * number-of-column-header-rows."]}
     {:name :max-width                 :required false                    :type "string"                        :validate-fn string?                        :description "standard CSS max-width setting of the entire table. Literally constrains the table to the given width so that if the table is wider than this it will add scrollbars. Ignored if value is larger than the combined width of all the columns and table padding."}
     {:name :max-rows                  :required false                    :type "integer"                       :validate-fn number?                        :description "The maximum number of rows to display in the table without scrolling. If not provided will take up all available vertical space."}
     {:name :table-row-line-color      :required false :default "#EAEEF1" :type "string"                        :validate-fn string?                        :description "The CSS color of the lines between rows."}
     {:name :on-click-row              :required false                    :type "function"                      :validate-fn ifn?                           :description "This function is called when the user clicks a row. Called with the row index. Do not use for adjusting row styles, use styling instead."}
     {:name :on-enter-row              :required false                    :type "function"                      :validate-fn ifn?                           :description "This function is called when the user's mouse pointer enters a row. Called with the row index. Do not use for adjusting row styles, use styling instead."}
     {:name :on-leave-row              :required false                    :type "function"                      :validate-fn ifn?                           :description "This function is called when the user's mouse pointer leaves a row. Called with the row index. Do not use for adjusting row styles, use styling instead."}
     {:name :row-height                :required false :default 31        :type "integer"                       :validate-fn number?                        :description "px height of each row."}
     {:name :table-padding             :required false :default 19        :type "integer"                       :validate-fn number?                        :description "Padding in pixels for the entire table."}
     {:name :header-renderer           :required false                    :type "columns -> hiccup"             :validate-fn ifn?                           :description "This function returns the hiccup to render the column headings."}
     {:name :row-style                 :required false                    :type "map | function"                :validate-fn #(or (ifn? %) (map? %))        :description "CSS styles to add or override on each row."}
     {:name :cell-style                :required false                    :type "map | function"                :validate-fn #(or (ifn? %) (map? %))        :description "CSS styles to add or override on each cell."}
     {:name :class                     :required false                    :type "string"                        :validate-fn string?                        :description "CSS class names, space separated (applies to the outer container)."}
     {:name :parts                     :required false                    :type "map"                           :validate-fn (parts? simple-v-table-parts)  :description "See Parts section below."}]))

(defn simple-v-table
  "Render a v-table and introduce the concept of columns (provide a spec for each).
  Of the nine possible sections of v-table, this table only supports four:
  top-left (1), row-headers (2), col-headers (4) and rows (5)
  Note that row-style and cell-style can either be a style map or functions which return a style map:
   - (row-style row)
   - (cell-style row col)
  where row is the data for that row and col is the definition map for that column
  "
  [& {:keys [model columns fixed-column-count table-padding table-row-line-color fixed-column-border-color]
      :or   {fixed-column-count        0
             table-padding             19
             table-row-line-color      "#EAEEF1"
             fixed-column-border-color "#BBBEC0"}
      :as   args}]
  {:pre [(validate-args-macro simple-v-table-args-desc args "simple-v-table")]}
  (let [fcc-bounded            (min fixed-column-count (count columns))
        fixed-cols             (subvec columns 0 fcc-bounded)
        content-cols           (subvec columns fcc-bounded (count columns))
        fixed-content-width    (reduce #(+ %1 (:width %2)) 0 fixed-cols)
        content-width          (reduce #(+ %1 (:width %2)) 0 content-cols)
        table-border-style     (str "1px solid " table-row-line-color)
        fixed-col-border-style (str "1px solid " fixed-column-border-color)
        actual-table-width     (+ fixed-content-width
                                  (when (pos? fixed-column-count) 1) ;; 1 border width (for fixed-col-border)
                                  content-width
                                  v-table/scrollbar-tot-thick
                                  (* 2 table-padding)
                                  2) ;; 2 border widths
        sort-by-column         (reagent/atom nil)]
    (fn [& {:keys [on-click-row on-enter-row on-leave-row header-renderer max-rows column-header-height row-height max-width
                   cell-style row-style class parts]
            :or   {column-header-height      31
                   row-height                31
                   header-renderer           column-headers}
            :as   args}]
      {:pre [(validate-args-macro simple-v-table-args-desc args "simple-v-table")]}
      (let [internal-model (reagent/track ;; TODO: Test if this works when external model is dynamic
                             (fn []
                               (if-let [{:keys [key-fn comp order] :or {comp compare}} @sort-by-column]
                                 (do
                                   (let [sorted (sort-by key-fn comp (deref-or-value model))]
                                     (if (= order :desc)
                                       (vec (reverse sorted))
                                       (vec sorted))))
                                 (deref-or-value model))))]
        [box/box
         :class (str "rc-simple-v-table-wrapper " (get-in parts [:simple-wrapper :class]))
         :style (merge {:flex             (if max-rows "none" "1")
                        :background-color "white"
                        :padding          (px table-padding)
                        :max-width        (or max-width (px actual-table-width)) ;; Removing actual-table-width would make the table stretch to the end of the page
                        :border           table-border-style
                        :border-radius    "3px"}
                       (get-in parts [:simple-wrapper :style]))
         :attr  (get-in parts [:simple-wrapper :attr])
         :child [v-table/v-table
                 :virtual?                true
                 :model                   internal-model

                 :row-height              row-height
                 :row-content-width       content-width
                 :row-header-renderer     (partial row-columns fixed-cols on-click-row on-enter-row on-leave-row row-height row-style cell-style table-row-line-color)
                 :row-renderer            (partial row-columns content-cols on-click-row on-enter-row on-leave-row row-height row-style cell-style table-row-line-color)

                 :column-header-height    column-header-height
                 :top-left-renderer       (partial header-renderer fixed-cols   parts sort-by-column) ;; Used when there are fixed columns
                 :column-header-renderer  (partial header-renderer content-cols parts sort-by-column)

                 ;:max-width               (px (or max-width (+ fixed-content-width content-width v-table/scrollbar-tot-thick)))

                 :max-row-viewport-height (when max-rows (* max-rows row-height))

                 :class                   class

                 :parts                   (cond-> (->
                                                    ;; Remove the parts that are exclusive to simple-v-table, or v-table part
                                                    ;; validation will fail:
                                                    (apply dissoc (into [parts] simple-v-table-exclusive-parts))
                                                    ;; Inject styles, if not set already, into parts. merge is not safe as it is not
                                                    ;; recursive so e.g. simply setting :attr would delete :style map.
                                                    (assoc-in-if-empty [:wrapper :style :font-size] "13px")
                                                    (assoc-in-if-empty [:wrapper :style :cursor] "default"))

                                                  (pos? fixed-column-count)
                                                  (->
                                                    (assoc-in-if-empty [:top-left :style :border-right] fixed-col-border-style)
                                                    (assoc-in-if-empty [:row-headers :style :border-right] fixed-col-border-style)))]]))))
