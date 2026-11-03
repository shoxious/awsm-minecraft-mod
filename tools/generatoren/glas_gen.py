import json
M=lambda i:"minecraft:"+i
G=lambda x,y,z:{"action":"goto","pos":[x,y,z]}
U=lambda pos,face:{"action":"use","item":M("stick"),"click":pos,"face":face}
raus=[G(-1413,119,1041),U([-1413,119,1042],"north"),G(-1413,119,1044),U([-1413,119,1042],"south"),G(-1418,119,1044)]
rein=[G(-1418,119,1044),G(-1413,119,1044),U([-1413,119,1042],"south"),G(-1413,119,1041),U([-1413,119,1042],"north")]
S=raus+[G(-1418,119,1049),G(-1419,119,1049),G(-1419,124,1042),G(-1419,124,1040),G(-1423,124,1039),G(-1423,129,1034),
  U([-1423,129,1032],"south"),G(-1423,129,1030),U([-1423,129,1032],"north"),G(-1423,129,1027),G(-1444,129,1027),
  {"action":"holen","pos":[-1443,130,1024],"item":M("glass"),"count":20},
  G(-1423,129,1027),G(-1423,129,1030),U([-1423,129,1032],"north"),G(-1423,129,1034),U([-1423,129,1032],"south"),
  G(-1423,124,1039),G(-1419,124,1040),G(-1419,124,1042),G(-1419,119,1049),G(-1418,119,1049)]+rein
for c in (-1413,-1403,-1423,-1433,-1443):
    S.append(G(c,119,1041))
    for x in (c-3,c+3):
        for y in (120,122):
            S.append({"action":"break","pos":[x,y,1043]})
        S.append({"action":"place","item":M("glass"),"pos":[x,120,1043],"click":[x,119,1043],"face":"up","hit":[0.5,1.0,0.5]})
        S.append({"action":"place","item":M("glass"),"pos":[x,122,1043],"click":[x,123,1043],"face":"down","hit":[0.5,0.0,0.5]})
S.append(G(-1410,119,1038)); S.append({"action":"look","yaw":0.0,"pitch":-3.0})
nacht=raus+[G(-1418,119,1053),G(-1423,119,1053),G(-1423,119,1055),U([-1423,121,1053],"south"),G(-1423,119,1049)]
morgen=[G(-1423,119,1050),G(-1423,119,1055),G(-1423,119,1053),G(-1418,119,1053)]+rein
json.dump({"steps":S,"nacht":nacht,"morgen":morgen,"bett":[-1425,119,1047]},open("glas.json","w"));print(len(S))
