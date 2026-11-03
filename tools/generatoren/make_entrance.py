import json,sys
sys.path.insert(0, sys.argv[1]); from hobbit_gen import *
sp=sys.argv[1]
name,x0,z0,dvx,dvz,uvx,uvz,wool,stripped,kamin=sys.argv[2],int(sys.argv[3]),int(sys.argv[4]),int(sys.argv[5]),int(sys.argv[6]),int(sys.argv[7]),int(sys.argv[8]),sys.argv[9],sys.argv[10]=="1",int(sys.argv[11])
scan=sys.argv[12]; extra=json.loads(sys.argv[13]) if len(sys.argv)>13 else []
W,w,B,D,solid,clicks,step_for,counts=build(name,x0,z0,(dvx,dvz),(uvx,uvz),wool,stripped,kamin,scan,[tuple(p) for p in extra])
M=lambda i:"minecraft:"+i
g=lambda p:{"action":"goto","pos":list(p)}
def solve(blocks):
    out=[]; rem=list(blocks)
    while rem:
        prog=False
        for b in list(rem):
            pos=w(b[0],b[1],b[2])
            for c in clicks(pos,b[3],b[4]):
                if tuple(c[0]) in solid:
                    out.append((b,c)); solid.add(pos); rem.remove(b); prog=True; break
        if not prog:
            print("NICHT setzbar:",[(w(b[0],b[1],b[2]),b[3]) for b in rem], file=sys.stderr); break
    return out
S=[]
# Phase 0: Hindernisse weg
S.append(g(w(-2,119,-2)))
S.append({"action":"use","item":M("netherite_axe"),"click":list(w(-2,118,-2)),"face":"up"})
for p in extra: S.append({"action":"break","pos":list(p)})
# Phase A: Boden (Kies, Bretter) – Stand seitlich
for p in [w(*t) for t in digs() if t[1]==118 and t[0]<=2]:
    S.append({"action":"break","pos":list(p)})
S.append({"action":"use","item":M("netherite_shovel"),"click":list(w(-2,118,-2)),"face":"up"})
A=[b for b in B if b[1]==118]
for b,c in solve(A): S.append(step_for(b,c))
# Phase B: Front d0/d1, Dach, Kamin, Vordach – zwei Standpunkte
front=[b for b in B if b[0] in (-1,0,1) and b[1]>=119 and b[3] not in ("door",)]
left=[b for b in front if b[2]<=0]; right=[b for b in front if b[2]>0]
for stand,blk in ((w(-2,119,-3),left),(w(-2,119,3),right)):
    S.append(g(stand))
    for b,c in solve(blk):
        S.append(step_for(b,c))
        if b[3]=="disc": S.append({"action":"use","item":M("netherite_axe"),"click":list(w(b[0],b[1],b[2])),"face":FACE[(-dvx,-dvz)]})
# Phase C: Kellerhals (von innen)
S+= [g(w(-2,119,0)), g(w(2,119,0))]
kh=[b for b in B if 2<=b[0]<=6 and b[1]>=119]
for b,c in solve(kh): S.append(step_for(b,c))
# Phase D: Treppe hinab
stair={b[0]:[x for x in B if x[3]=="stair" and x[4][1]=="-d" and x[0]==b[0]] for b in B if b[3]=="stair" and b[4][1]=="-d"}
def dig(d,ys): return [{"action":"break","pos":list(w(d,y,u))} for y in ys for u in (-1,0,1)]
S.append({"action":"use","item":M("netherite_shovel"),"click":list(w(2,118,0)),"face":"up"})
S+= dig(3,(118,117))+dig(4,(118,117,116))
for b,c in solve(stair[3]): S.append(step_for(b,c))
S.append(g(w(3,117,0))); S+= dig(5,(118,117,116,115))
for b,c in solve(stair[4]): S.append(step_for(b,c))
S.append(g(w(4,116,0))); S+= dig(6,(117,116,115,114))
for b,c in solve(stair[5]): S.append(step_for(b,c))
S.append(g(w(5,115,0))); S+= dig(7,(116,115,114,113))
for b,c in solve(stair[6]): S.append(step_for(b,c))
S.append(g(w(6,114,0)))
for b,c in solve(stair[7]): S.append(step_for(b,c))
for b,c in solve([b for b in B if b[3]=="torch_wall"]): S.append(step_for(b,c))
S+= [g(w(7,113,0)), g(w(8,113,0)), g(w(7,113,0)), g(w(5,115,0)), g(w(3,117,0)), g(w(2,119,0))]
# Phase E: Tuer + Garten
S.append(g(w(-1,119,-1)))
for b,c in solve([b for b in B if b[3]=="door"]): S.append(step_for(b,c))
S.append(g(w(-2,119,-2)))
for b,c in solve([b for b in B if b[3] in ("fence","gate","lantern_top")]): S.append(step_for(b,c))
json.dump(S, open(f"{sp}/{name}-steps.json","w"))
print(len(S),"Schritte"); print(dict(counts))
