import json
M=lambda i:"minecraft:"+i
P="spruce_planks"
T=[-1448,119,1042]
steps=[]
steps.append({"action":"craft","pos":T,"item":M(P),"grid":[M("spruce_log")]+[""]*8,"count":26})
g=[""]*9
for i in (0,3,4,6,7,8): g[i]=M(P)
steps.append({"action":"craft","pos":T,"item":M("spruce_stairs"),"grid":g,"count":5})
g=[""]*9
for i in (0,1,3,4,6,7): g[i]=M(P)
steps.append({"action":"craft","pos":T,"item":M("spruce_door"),"grid":g,"count":1})
D0,D1=-1396,-1395
H={"up":[0.5,1.0,0.5],"down":[0.5,0.0,0.5],"west":[0.0,0.5,0.5]}
def pl(item,pos,click,face,yaw=None,hit=None):
    s={"action":"place","item":M(item),"pos":pos,"click":click,"face":face,"hit":hit or H[face]}
    if yaw is not None: s["yaw"]=yaw
    return s
YU={-1:180.0,1:0.0}   # Treppe hohe Seite -u (Norden) / +u (Sueden)
route=[[-1440,119,1040],[-1420,119,1038],[-1405,119,1039]]
for p in route: steps.append({"action":"goto","pos":p})
for zc in (1041,1037,1033,1029,1025):
    steps.append({"action":"goto","pos":[-1398,119,zc]})
    for x in (D0,D1):
        for y in (122,121,120,119):
            for u in (-1,0,1):
                steps.append({"action":"break","pos":[x,y,zc+u]})
    # Scheibe d1
    for u in (-1,1):
        for y in (119,120):
            steps.append(pl(P,[D1,y,zc+u],[D1,y-1,zc+u],"up"))
    for u in (-1,0,1):
        steps.append(pl(P,[D1,121,zc+u],[D1,120,zc+u] if u else [D1,121,zc-1],"up" if u else "south",hit=None if u else [0.5,0.5,1.0]))
    for u in (-1,0,1):
        steps.append(pl(P,[D1,122,zc+u],[D1,121,zc+u],"up"))
    steps.append(pl("spruce_door",[D1,119,zc],[D1,118,zc],"up",yaw=-90.0))
    # d0 Rahmen
    for u in (-1,0,1):
        steps.append(pl(P,[D0,122,zc+u],[D1,122,zc+u],"west"))
    for u in (-1,1):
        steps.append(pl("spruce_stairs",[D0,119,zc+u],[D0,118,zc+u],"up",yaw=YU[u]))
        steps.append(pl("spruce_stairs",[D0,121,zc+u],[D0,122,zc+u],"down",yaw=YU[u]))
nacht=[{"action":"goto","pos":p} for p in ([-1398,119,1044],[-1398,119,1047],[-1410,119,1047],[-1418,119,1054])]+[
 {"action":"goto","pos":[-1423,119,1055]},
 {"action":"use","item":"minecraft:stick","click":[-1423,121,1053],"face":"south"},
 {"action":"goto","pos":[-1423,119,1049]}]
morgen=[{"action":"goto","pos":p} for p in ([-1423,119,1050],[-1423,119,1055],[-1418,119,1054],[-1410,119,1047],[-1398,119,1047],[-1398,119,1040])]
json.dump({"steps":steps,"nacht":nacht,"morgen":morgen,"bett":[-1425,119,1047]},open("tueren.json","w"))
print(len(steps))
