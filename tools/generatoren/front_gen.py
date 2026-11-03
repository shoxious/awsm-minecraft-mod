import json
M=lambda i:"minecraft:"+i
P="spruce_planks"
G=lambda x,y,z:{"action":"goto","pos":[x,y,z]}
H={"up":[0.5,1.0,0.5],"down":[0.5,0.0,0.5],"south":[0.5,0.5,1.0],"north":[0.5,0.5,0.0],"east":[1.0,0.5,0.5],"west":[0.0,0.5,0.5]}
def pl(item,pos,click,face,yaw=None):
    s={"action":"place","item":M(item),"pos":list(pos),"click":list(click),"face":face,"hit":H[face]}
    if yaw is not None: s["yaw"]=yaw
    return s
S=[]
# --- hinauf ins Haus, Material holen ---
S+= [G(-1420,119,1044),G(-1420,119,1043),G(-1418,119,1043),G(-1418,119,1049),G(-1419,119,1049),
     G(-1419,124,1042),G(-1419,124,1040),G(-1423,124,1039),G(-1423,129,1034),
     {"action":"use","item":M("stick"),"click":[-1423,129,1032],"face":"south"},
     G(-1423,129,1030),
     {"action":"use","item":M("stick"),"click":[-1423,129,1032],"face":"north"},
     G(-1423,129,1027),G(-1440,129,1027),{"action":"sortieren"},G(-1444,129,1027),{"action":"sortieren"},
     {"action":"holen","pos":[-1446,130,1031],"item":M("spruce_log"),"count":96},
     {"action":"holen","pos":[-1446,129,1031],"item":M("stick"),"count":8},
     {"action":"holen","pos":[-1443,130,1024],"item":M("glass_pane"),"count":56},
     G(-1433,129,1028)]
T=[-1436,129,1031]
S.append({"action":"craft","pos":T,"item":M(P),"grid":[M("spruce_log")]+[""]*8,"count":50})
g=[""]*9
for i in (0,3,4,6,7,8): g[i]=M(P)
S+=[G(-1433,129,1028),{"action":"craft","pos":T,"item":M("spruce_stairs"),"grid":g,"count":15}]
g=[""]*9
for i in (0,1,3,4,6,7): g[i]=M(P)
S+=[G(-1433,129,1028),{"action":"craft","pos":T,"item":M("spruce_door"),"grid":g,"count":2}]
# --- zurueck hinunter in die Halle ---
S+= [G(-1423,129,1027),G(-1423,129,1030),
     {"action":"use","item":M("stick"),"click":[-1423,129,1032],"face":"north"},
     G(-1423,129,1034),
     {"action":"use","item":M("stick"),"click":[-1423,129,1032],"face":"south"},
     G(-1423,124,1039),G(-1419,124,1040),G(-1419,124,1042),G(-1419,119,1049),G(-1418,119,1049),
     G(-1418,119,1043),G(-1418,119,1041)]
# --- Front z1043 (d0) / z1042 (d1), u = +x ---
D0,D1=1043,1042
YW,YE=90.0,-90.0           # Blick West / Ost
yaw_u={-1:YW,1:YE}         # Treppe hohe Seite Richtung -u / +u
def module(c):
    out=[G(c,119,1041)]
    # Scheibe d1 um die Tuer
    for u in (-1,1):
        out.append(pl(P,(c+u,119,D1),(c+u,118,D1),"up"))
        out.append(pl(P,(c+u,120,D1),(c+u,119,D1),"up"))
    out.append(pl(P,(c-1,121,D1),(c-1,120,D1),"up"))
    out.append(pl(P,(c+1,121,D1),(c+1,120,D1),"up"))
    out.append(pl(P,(c,121,D1),(c-1,121,D1),"east"))
    for u in (-1,0,1): out.append(pl(P,(c+u,122,D1),(c+u,121,D1),"up"))
    out.append(pl("spruce_door",(c,119,D1),(c,118,D1),"up",yaw=180.0))
    # Tuerring d0
    for u in (-1,0,1): out.append(pl(P,(c+u,122,D0),(c+u,122,D1),"south"))
    for u in (-1,1):
        out.append(pl("spruce_stairs",(c+u,119,D0),(c+u,118,D0),"up",yaw=yaw_u[u]))
        out.append(pl("spruce_stairs",(c+u,121,D0),(c+u,122,D0),"down",yaw=yaw_u[u]))
    # runde Fenster links/rechts
    for s in (-1,1):
        a,b,cc=(c-4,c-3,c-2) if s<0 else (c+2,c+3,c+4)
        for x in (a,b,cc): out.append(pl(P,(x,119,D0),(x,118,D0),"up"))
        out.append(pl("spruce_stairs",(a,120,D0),(a,119,D0),"up",yaw=YW))
        out.append(pl("glass_pane",(b,120,D0),(b,119,D0),"up"))
        out.append(pl("spruce_stairs",(cc,120,D0),(cc,119,D0),"up",yaw=YE))
        for x in (a,b,cc): out.append(pl("glass_pane",(x,121,D0),(x,120,D0),"up"))
        out.append(pl("glass_pane",(b,122,D0),(b,121,D0),"up"))
        out.append(pl("spruce_stairs",(a,122,D0),(a,123,D0),"down",yaw=YW))
        out.append(pl("spruce_stairs",(cc,122,D0),(cc,123,D0),"down",yaw=YE))
    return out
def post(x):
    return [pl("spruce_log",(x,y,D0),(x,y-1,D0),"up") for y in (119,120,121,122)]
S.append(G(-1403,119,1041))
S.append({"action":"break","pos":[-1400,119,1043]})
S+=module(-1403)+post(-1398)+post(-1408)
S+=module(-1413)+post(-1418)
S+=[G(-1423,119,1041),{"action":"break","pos":[-1423,118,1042]},pl(P,(-1423,118,1042),(-1423,117,1042),"up")]
S+=module(-1433)+post(-1428)+post(-1438)
S+=module(-1443)
S+=module(-1423)
S.append(G(-1423,119,1039)); S.append({"action":"look","yaw":0.0,"pitch":5.0})
nacht=[G(-1423,119,1041),G(-1423,119,1044),G(-1430,119,1044),G(-1430,119,1053),G(-1423,119,1053),G(-1423,119,1055),
       {"action":"use","item":M("stick"),"click":[-1423,121,1053],"face":"south"},G(-1423,119,1049)]
morgen=[G(-1423,119,1050),G(-1423,119,1055),G(-1423,119,1053),G(-1430,119,1053),G(-1430,119,1044),G(-1423,119,1044),G(-1423,119,1041)]
json.dump({"steps":S,"nacht":nacht,"morgen":morgen,"bett":[-1425,119,1047]},open("front.json","w"))
import collections
c=collections.Counter(s["item"] for s in S if s["action"]=="place")
print(len(S),dict(c))
