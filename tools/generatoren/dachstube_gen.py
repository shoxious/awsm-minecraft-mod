import json
M=lambda i:"minecraft:"+i
G=lambda x,y,z:{"action":"goto","pos":[x,y,z]}
def H(face):
    return {"up":[0.5,1.0,0.5],"down":[0.5,0.0,0.5],"east":[1.0,0.5,0.5],"west":[0.0,0.5,0.5],"south":[0.5,0.5,1.0],"north":[0.5,0.5,0.0]}[face]
def pl(item,pos,click=None,face="up",yaw=None,hit=None,pitch=None):
    if click is None: click=[pos[0],pos[1]-1,pos[2]]
    s={"action":"place","item":M(item),"pos":list(pos),"click":list(click),"face":face,"hit":hit or H(face)}
    if yaw is not None: s["yaw"]=yaw
    if pitch is not None: s["pitch"]=pitch
    return s
def use(item,click,face="up",yaw=None):
    s={"action":"use","item":M(item),"click":list(click),"face":face}
    if yaw is not None: s["yaw"]=yaw
    return s
def holen(pos,item,n): return {"action":"holen","pos":pos,"item":M(item),"count":n}
def craft(item,grid,n):
    g=[(M(x) if x else "") for x in grid]+[""]*(9-len(grid))
    return [G(-1433,129,1028),{"action":"craft","pos":[-1436,129,1031],"item":M(item),"grid":g,"count":n}]

# ============ Teil 1: holen + craften (im Haus) ============
A=[G(-1423,129,1027),G(-1444,129,1027),
   holen([-1446,130,1031],"spruce_log",72), holen([-1446,130,1031],"cherry_log",7),
   holen([-1446,129,1031],"stick",4),
   holen([-1446,129,1027],"emerald_block",7), holen([-1446,129,1027],"lapis_block",7),
   holen([-1446,130,1027],"iron_ingot",4),
   holen([-1443,130,1024],"red_dye",10), holen([-1443,130,1024],"white_dye",10), holen([-1443,130,1024],"magenta_dye",14),
   holen([-1445,130,1024],"string",9),
   holen([-1445,129,1024],"torch",4), holen([-1445,129,1024],"flint_and_steel",1),
   holen([-1443,129,1031],"honeycomb",7),
   G(-1440,129,1027),
   holen([-1440,129,1031],"white_wool",88), holen([-1440,129,1031],"pink_wool",5),
   holen([-1440,129,1031],"pink_carpet",2), holen([-1440,129,1031],"magenta_carpet",3),
   holen([-1439,130,1024],"lantern",6), holen([-1439,130,1024],"candle",3)]
p,s="spruce_planks","spruce_slab"
A+=craft(p,["spruce_log"],36)+craft(p,["spruce_log"],36)
A+=craft(s,[p,p,p],13)
A+=craft("barrel",[p,s,p,p,"",p,p,s,p],39)
A+=craft("crafting_table",[p,p,"",p,p],1)
A+=craft("loom",["string","string","",p,p],1)
A+=craft("white_bed",["white_wool"]*3+[p]*3,2)
cp="cherry_planks"
A+=craft(cp,["cherry_log"],7)
A+=craft("cherry_stairs",[cp,"","",cp,cp,"",cp,cp,cp],2)
A+=craft("cherry_trapdoor",[cp,cp,cp,cp,cp,cp],1)
A+=craft("cherry_slab",[cp,cp,cp],1)
A+=craft("chiseled_bookshelf",[cp,cp,cp,"cherry_slab","cherry_slab","cherry_slab",cp,cp,cp],1)
A+=craft("pink_dye",["red_dye","white_dye"],10)
A+=craft("pink_bed",["white_bed","pink_dye"],2)
A+=craft("white_carpet",["white_wool","white_wool"],35)
w="white_carpet"
A+=craft("pink_carpet",[w,w,w,w,"pink_dye",w,w,w,w],11)
A+=craft("magenta_carpet",[w,w,w,w,"magenta_dye",w,w,w,w],2)
A+=craft("magenta_wool",["white_wool","magenta_dye"],12)
mw="magenta_wool"
A+=craft("magenta_banner",[mw,mw,mw,mw,mw,mw,"","stick",""],2)
A+=craft("candle",["string","","","honeycomb"],7)
A+=craft("pink_candle",["candle","pink_dye"],6)
A+=craft("iron_nugget",["iron_ingot"],4)
n="iron_nugget"
A+=craft("lantern",[n,n,n,n,"torch",n,n,n,n],4)

# ============ Teil 2: Dachboden ============
B=[G(-1423,129,1027),G(-1410,129,1027),G(-1410,129,1025),
   {"action":"klettern","pos":[-1409,133,1025],"yaw":-90},G(-1410,133,1026),G(-1410,133,1027)]
cur=[None]
def at(x,z=1027):
    if cur[0]!=(x,z):
        cur[0]=(x,z); B.append(G(x,133,z))
# Licht: Laternen unter den First, dann Fackeln weg
for x in (-1408,-1414,-1420,-1425,-1431,-1437,-1443):
    at(x); B.append(pl("lantern",[x,136,1027],[x,137,1027],"down"))
for (x,y,z,st) in ((-1443,133,1027,-1442),(-1435,133,1027,-1436),(-1427,133,1027,-1428),(-1446,133,1024,-1444),(-1441,134,1025,-1441),(-1443,134,1030,-1442),(-1441,134,1030,-1441),(-1446,133,1030,-1444)):
    at(st); B.append({"action":"break","pos":[x,y,z]})
# Zone A West
at(-1440,1027)
for x in range(-1441,-1436): B.append(pl("pink_wool",[x,133,1024],pitch=50.0))
for x in (-1441,-1439,-1437): B.append(pl("cherry_slab",[x,133,1025],[x,133,1024],"south",hit=[0.5,0.75,1.0]))
for x in (-1440,-1438): B.append(pl("pink_bed",[x,133,1026],yaw=180.0))
for x in (-1441,-1439,-1437): B.append(pl("pink_candle",[x,134,1025],[x,133,1025],"up"))
at(-1444,1027)
B.append(pl("chiseled_bookshelf",[-1446,133,1026],yaw=90.0))
B.append(pl("cherry_stairs",[-1446,133,1027],yaw=90.0))
B.append(pl("cherry_stairs",[-1446,133,1028],yaw=90.0))
B.append(pl("cherry_slab",[-1446,133,1029],[-1447,133,1029],"east",hit=[1.0,0.75,0.5]))
B.append(pl("pink_candle",[-1446,134,1029],[-1446,133,1029],"up"))
B.append(use("pink_candle",[-1446,134,1029]))
B.append(use("pink_candle",[-1446,134,1029]))
for z in (1026,1027,1028,1029):
    for x in (-1445,-1444,-1443):
        if (x,z)==(-1444,1027): continue
        B.append(pl("magenta_carpet",[x,133,z]))
at(-1442,1028); B.append(pl("magenta_carpet",[-1444,133,1027]))
at(-1433,1029); B.append(pl("loom",[-1433,133,1031],yaw=0.0))
# Zone B Mitte
at(-1422,1026)
for i,x in enumerate(range(-1427,-1417)):
    B.append(pl("emerald_block" if i%2==0 else "lapis_block",[x,133,1024],pitch=50.0))
for x,b in ((-1426,"emerald_block"),(-1421,"emerald_block"),(-1424,"lapis_block"),(-1419,"lapis_block")):
    B.append(pl(b,[x,133,1025],pitch=50.0))
for x in (-1426,-1421,-1424,-1419): B.append(pl("candle",[x,134,1025],[x,133,1025],"up"))
at(-1422,1028)
for x in (-1424,-1423,-1422,-1421): B.append(pl("cherry_stairs",[x,133,1030],yaw=180.0))
B.append(pl("cherry_trapdoor",[-1425,133,1030],yaw=-90.0)); B.append(use("cherry_trapdoor",[-1425,133,1030],"up"))
B.append(pl("cherry_trapdoor",[-1420,133,1030],yaw=90.0)); B.append(use("cherry_trapdoor",[-1420,133,1030],"up"))
for x in (-1424,-1421): B.append(pl("lantern",[x,133,1031],pitch=50.0))
for x in range(-1425,-1419): B.append(pl("magenta_carpet",[x,133,1029]))
# Zone C Ost (Faesser nur hier, Luke x -1410..-1408 frei)
for xs,z,sz in ((list(range(-1417,-1410))+list(range(-1407,-1398)),1024,1026),(list(range(-1417,-1398)),1031,1029)):
    for x in xs:
        st=-1415 if x<=-1411 else (-1405 if x<=-1404 else -1401)
        at(st,sz); B.append(pl("barrel",[x,133,z],pitch=60.0))
at(-1401,1027)
B.append(pl("crafting_table",[-1399,133,1026]))
B.append(pl("barrel",[-1399,133,1029],yaw=90.0,pitch=0.0))
B.append(pl("magenta_banner",[-1399,135,1026],[-1398,135,1026],"west"))
B.append(pl("magenta_banner",[-1399,135,1029],[-1398,135,1029],"west"))
# letzte Fackeln (Ost/Mitte) jetzt weg, Laternen hängen schon
B.append(pl("lantern",[-1402,136,1027],[-1402,137,1027],"down"))
for (x,y,z,st) in ((-1402,133,1027,-1401),(-1399,134,1025,-1401),(-1399,134,1029,-1401),(-1410,133,1027,-1411),(-1418,133,1027,-1419)):
    at(st); B.append({"action":"break","pos":[x,y,z]})
# Kerzen anzünden
at(-1422,1026)
for x in (-1426,-1421,-1424,-1419): B.append(use("flint_and_steel",[x,134,1025]))
at(-1440,1027)
for x in (-1441,-1439,-1437): B.append(use("flint_and_steel",[x,134,1025]))
at(-1444,1028); B.append(use("flint_and_steel",[-1446,134,1029]))
# Teppich-Laufweg zum Schluss, von West nach Ost rückwärts laufend
for x in range(-1442,-1398):
    if x%4==0 or x==-1442: at(x+2 if x+2<=-1399 else x,1029)
    for z in (1027,1028): B.append(pl("pink_carpet",[x,133,z],pitch=55.0))
at(-1409,1027); B.append(pl("pink_carpet",[-1409,133,1026]))
B+=[G(-1410,133,1027),G(-1410,133,1026),G(-1409,129,1025),G(-1410,129,1027)]
nacht=[G(-1410,133,1027),G(-1410,133,1026),G(-1409,129,1025),G(-1410,129,1027),G(-1401,129,1027)]
morgen=[G(-1410,129,1027),G(-1410,129,1025),{"action":"klettern","pos":[-1409,133,1025],"yaw":-90},G(-1410,133,1026),G(-1410,133,1027)]
json.dump({"steps":A},open("dach1.json","w"))
json.dump({"steps":B,"nacht":nacht,"morgen":morgen,"bett":[-1399,129,1027]},open("dach2.json","w"))
import collections
print(len(A),len(B),collections.Counter(s["item"] for s in B if s["action"]=="place"))
