(ns interpreter.default-test
  (:refer-clojure :exclude [eval true?])
  (:require [clojure.test :refer :all]
            [interpreter.type :refer :all]
            [interpreter.impl.default :refer :all]))

(defmacro expect
  [a b]
  `(is (= ~a ~b)))

(def eval #'interpreter.impl.default/eval)                  ; test private

(deftest primitive-eval
  (testing
    [(expect '3 (eval '3))
     (expect 'TRUE (eval 'TRUE))
     (expect 2 (eval '(+ 1 1)))
     (expect 5 (eval '(+ (+ 1 2) 1 1)))
     (expect 3 (eval-program '[3]))]))

(deftest env-eval
  (testing
    [(expect 2 (eval 'a {'a 2}))

     (expect 3
             (eval-program
               '[(def a 3)
                 a]))

     ; test state inside do
     (expect 4
             (eval-program
               '[(do (def x 2)
                     (+ x x))]))

     (expect {'a 3}
             (:env (#'interpreter.impl.default/eval-sexp '(def a 3) {})))]))

(deftest seq-eval
  (testing
    [(expect '2
             (eval '(if TRUE 2 3)))

     (expect '3
             (eval '(if FALSE 2 3)))

     (expect '(if test
                result)
             (cond->if '(cond test result)))

     (expect '(if c1
                r1
                (if c2
                  r2))
             (cond->if '(cond c1 r1
                              c2 r2)))

     (expect 3
             (eval '(if TRUE 3)))

     (expect '(if TRUE 3)
             (cond->if '(cond TRUE
                              3)))

     (expect 3
             (eval '(cond TRUE 3)))

     (expect 2
             (eval '((fn [x] x) 2)))

     (expect 4
             (eval-program '[(def f (fn [y] y))
                             (f 4)]))

     (expect 25
             (eval-program '[(def square (fn [x] (* x x)))
                             (+ (square 3) (square 4))]))

     (expect 1
             (eval-program '[(if TRUE 1 2)]))

     (expect 2
             (eval-program '[(if FALSE 1 2)]))

     (expect 'NIL
             (eval '(if FALSE 1)))

     (expect 1
             (eval-program '[(def a 1)
                             (if TRUE 1)
                             a]))

     (expect 8
             (eval-program '[(def add-n (fn [n]
                                          (fn [r]
                                            (+ n r))))
                             ((add-n 3) 5)]))

     (expect '((fn [a c]
                 (+ a c))
                b d)
             (let->fn '(let [a b
                             c d]
                         (+ a c))))

     (expect 3
             (eval '(let [a 3]
                      a)))

     (expect 5
             (eval '(let [f (fn [x] (+ x 2))]
                      (f 3))))]))

(deftest scheme-syntax
  (let [preamble '[(def cons (fn [x y]
                               (fn [m]
                                 (cond (= m 0) x
                                       (= m 1) y))))
                   (def car (fn [z] (z 0)))
                   (def cdr (fn [z] (z 1)))
                   (def t (cons 3 4))]]
    (testing
      [(expect 'TRUE
               (eval-program (concat preamble
                                     '[(= (car t) 3)])))

       (expect 'TRUE
               (eval-program (concat preamble
                                     '[(= (cdr t) 4)])))

       (expect 7
               (eval '(((fn [x]
                          (fn [y] (+ x y)))
                         3)
                        4)))])))

(deftest factorial-eval
  (testing
    [
     (expect 1
             (eval-program '[(defn factorial [n]
                               (if (= n 1)
                                 1
                                 (* n (factorial (- n 1)))))
                             (factorial 1)]))

     (expect 6
             (eval-program '[(defn factorial [n]
                               (if (= n 1)
                                 1
                                 (* n (factorial (- n 1)))))
                             (factorial 3)]))
     ]))