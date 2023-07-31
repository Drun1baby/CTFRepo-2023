from Crypto.Util.number import *
from random import *
from sage.all import *

flag = b'--hidden_message--'
data1 = getPrime(256)
data2 = getPrime(256)
m = bytes_to_long(flag)+data2
prec = 600
ring = RealField(prec)
data3 = ring(data1) / ring(data2)
print(data3)

while True:
    p = randint(2**255, data1)
    q = randint(2**255, data2)
    if isPrime(p) and isPrime(q) and p!=q:
        break

n = p*q
e = 65537
leak = pow(p-q, data1, data1*data2)
c = pow(m, e, n)
print(c)
print(n)
print(leak)


'''
1.42870767357206600351348423521722279489230609801270854618388981989800006431663026299563973511233193052826781891445323183272867949279044062899046090636843802841647378505716932999588
1046004343125860480395943301139616023280829254329678654725863063418699889673392326217271296276757045957276728032702540618505554297509654550216963442542837
2793178738709511429126579729911044441751735205348276931463015018726535495726108249975831474632698367036712812378242422538856745788208640706670735195762517
1788304673303043190942544050868817075702755835824147546758319150900404422381464556691646064734057970741082481134856415792519944511689269134494804602878628
'''