"""Independent reference values for the financial engine tests.

Uses Python's decimal module (60 digits) with closed-form formulas written differently
from the Kotlin implementation (e.g. annuity-immediate/annuity-due formulas with negative
exponents) and root finding by plain bisection over a bracket found by hand.
"""
from decimal import Decimal as D, getcontext, ROUND_HALF_EVEN

getcontext().prec = 60


def p(label, value, digits=20):
    print(f"{label} = {format(value, f'.{digits}f')}")


def pw(base, expo):  # exp/ln based power (works for fractional exponents)
    return (D(expo) * D(base).ln()).exp()


# ---- TVM (closed forms) ----
r = D("0.005")
n = 360
pv = D(100000)
pmt_end = -pv * r / (1 - pw(1 + r, -n))
p("PMT loan 100000 360 0.5% end", pmt_end)
p("PMT loan 100000 360 0.5% begin", pmt_end / (1 + r))

r = D("0.0075")
p("FV savings 200/mo 120 0.75%", D(200) * (pw(1 + r, 120) - 1) / r)
r = D("0.01")
p("PV annuity 500 x 60 at 1%", D(500) * (1 - pw(1 + r, -60)) / r)
p("n for 1000 -> 2000 at 1%", D(2).ln() / D("1.01").ln())
# n with payments: loan 10000, pmt 300, r=1%: n = -ln(1 - L r / pmt) / ln(1+r)
p("n loan 10000 pmt 300 1%", -(1 - D(10000) * r / 300).ln() / (1 + r).ln())
p("FV 1000 at 10% for 2.5 periods", D(1000) * pw(D("1.1"), D("2.5")))
p("PMT zero rate -1200 / 12", D(1200) / 12)

# ---- rate by bisection for loan 100000, n=360, pmt=-599.55 (end) ----


def f(rate, pv=D(100000), pmt=D("-599.55"), n=360):
    return pv * pw(1 + rate, n) + pmt * (pw(1 + rate, n) - 1) / rate


lo, hi = D("0.001"), D("0.02")
for _ in range(200):
    mid = (lo + hi) / 2
    if f(lo) * f(mid) <= 0:
        hi = mid
    else:
        lo = mid
p("i loan 100000 360 -599.55 (percent)", (lo + hi) / 2 * 100, 14)

# ---- NPV / IRR ----


def npv(rate, flows):
    return sum(c / pw(1 + rate, k) for k, c in enumerate(flows))


def irr_bisect(flows, lo, hi):
    flo = npv(lo, flows)
    for _ in range(300):
        mid = (lo + hi) / 2
        fm = npv(mid, flows)
        if (flo < 0) == (fm < 0):
            lo, flo = mid, fm
        else:
            hi = mid
    return (lo + hi) / 2


A = [D(-1000), D(300), D(400), D(500)]
p("NPV A at 10%", npv(D("0.1"), A))
p("NPV A at 0%", sum(A))
p("IRR A percent", irr_bisect(A, D("0.0"), D("1")) * 100, 12)
B = [D(-5000)] + [D(1500)] * 4
p("NPV B at 8%", npv(D("0.08"), B))
p("IRR B percent", irr_bisect(B, D("0.0"), D("1")) * 100, 12)
C = [D(-100), D(230), D(-132)]
p("IRR C low root percent", irr_bisect(C, D("0.05"), D("0.15")) * 100, 12)
p("IRR C high root percent", irr_bisect(C, D("0.15"), D("0.3")) * 100, 12)
# flows with two sign changes but a single root: scan to make sure
E = [D(-1000), D(1500), D(-100)]
import itertools
signs = []
prev = None
roots = 0
x = D("-0.9")
while x < 10:
    v = npv(x, E)
    if prev is not None and (prev < 0) != (v < 0):
        roots += 1
        print("E root near", x)
    prev = v
    x += D("0.005")
print("E roots", roots)
p("IRR E percent", irr_bisect(E, D("0.0"), D("1")) * 100, 12)

# ---- Amortization schedule with half-even rounding each period ----
bal = D(10000)
r = D("0.01")
pmt = (D(10000) * r / (1 - pw(1 + r, -12))).quantize(D("0.01"), rounding=ROUND_HALF_EVEN)
print("pmt rounded", pmt)
tot_i = D(0)
tot_p = D(0)
for k in range(1, 13):
    i = (bal * r).quantize(D("0.01"), rounding=ROUND_HALF_EVEN)
    pr = pmt - i
    bal -= pr
    if k in (1, 2, 12) or k <= 3:
        print(k, i, pr, bal)
    if k <= 6:
        tot_i += i
        tot_p += pr
print("first 6: interest", tot_i, "principal", tot_p, "balance after 6", None)
bal = D(10000)
for k in range(1, 7):
    i = (bal * r).quantize(D("0.01"), rounding=ROUND_HALF_EVEN)
    bal -= pmt - i
print("balance after 6", bal)

# ---- rate conversions ----
p("effective annual from nominal 12% monthly", (pw(D("1.01"), 12) - 1) * 100, 18)
p("monthly equivalent of 10% annual", (pw(D("1.1"), D(1) / 12) - 1) * 100, 18)
p("nominal 12-compounding from 10% eff", D(12) * (pw(D("1.1"), D(1) / 12) - 1) * 100, 18)
p("annual equivalent of 1% monthly", (pw(D("1.01"), 12) - 1) * 100, 18)
p("annual equivalent of 0.05% daily", (pw(D("1.0005"), 365) - 1) * 100, 18)
p("quarterly equivalent of 12% annual", (pw(D("1.12"), D("0.25")) - 1) * 100, 18)
p("effective from nominal 24% quarterly", (pw(D("1.06"), 4) - 1) * 100, 18)
