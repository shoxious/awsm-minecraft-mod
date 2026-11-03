import json
M=lambda i:"minecraft:"+i
G=lambda x,z:{"action":"goto","pos":[x,119,z]}
B=lambda x,y,z:{"action":"break","pos":[x,y,z]}
def pl(item,pos,click,face,hit,yaw=None):
    s={"action":"place","item":M(item),"pos":pos,"click":click,"face":face,"hit":hit}
    if yaw is not None: s["yaw"]=yaw
    return s
blocked={-1442,-1419,-1404,-1403,-1402,-1400}
S=[G(-1430,1040),G(-1446,1041),
   {"action":"craft","pos":[-1448,119,1042],"item":M("spruce_planks"),"grid":[M("spruce_log")]+[""]*8,"count":10},
   {"action":"craft","pos":[-1448,119,1042],"item":M("spruce_slab"),"grid":[M("spruce_planks")]*3+[""]*6,"count":5}]
for c in (-1443,-1433,-1423,-1413,-1403):
    S.append(G(c,1040))
    wins=[(c-4,c-3,c-2),(c+2,c+3,c+4)]
    for a,b,cc in wins:
        for x in (a,cc):
            for y in (120,122): S.append(B(x,y,1042))
        for x in (a,b,cc):
            for y in (120,122): S.append(B(x,y,1043))
    S.append(G(c,1041))
    # Tuerring-Luecken y120 schliessen
    for u in (-1,1):
        S.append(pl("spruce_planks",[c+u,120,1043],[c+u,120,1042],"south",[0.5,0.5,1.0]))
    for a,b,cc in wins:
        for x in (a,b,cc):
            S.append(pl("glass_pane",[x,120,1043],[x,119,1043],"up",[0.5,1.0,0.5]))
        for x in (a,b,cc):
            S.append(pl("spruce_planks",[x,122,1043],[x,123,1043],"down",[0.5,0.0,0.5]))
        for x in (a,b,cc):
            if x in blocked: continue
            S.append(pl("spruce_slab",[x,119,1044],[x,119,1043],"south",[0.5,0.75,1.0]))
S+=[G(-1410,1038),{"action":"look","yaw":0.0,"pitch":5.0}]
d=json.load(open('ecken.json'))
json.dump({"steps":S,"nacht":d["nacht"],"morgen":d["morgen"],"bett":d["bett"]},open('fenster2.json','w'))
print(len(S))
