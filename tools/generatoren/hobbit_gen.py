import json,sys,collections
# ---- Eingang-Beschreibung in lokalen Koordinaten (d = Tiefe, u = quer) ----
def spec(wool, disc_stripped, kamin_u):
    B=[]   # (d,y,u,kind,arg)
    P=lambda d,y,u,k,a=None: B.append((d,y,u,k,a))
    # y118 Boden
    for d in (-3,-2,-1,0): P(d,118,0,"gravel")
    P(1,118,0,"full","spruce_planks")
    for u in (-1,0,1): P(2,118,u,"full","spruce_planks")
    # Front d0 / d1
    for y in (119,120,121,122):
        for u in (-5,5): P(0,y,u,"log_v"); P(1,y,u,"log_v")
    for u in (-4,-3,-2,2,3,4): P(0,119,u,"full","spruce_planks")
    P(0,119,-1,"stair",("bottom","-u")); P(0,119,1,"stair",("bottom","+u"))
    for s in (-1,1):   # Fenster links (s=-1 -> u -4..-2), rechts (s=+1 -> u 2..4)
        a,b,c=( -4,-3,-2) if s<0 else (2,3,4)
        P(0,120,a,"stair",("bottom","-u")); P(0,120,b,"pane"); P(0,120,c,"stair",("bottom","+u"))
        for u in (a,b,c): P(0,121,u,"pane")
        P(0,122,a,"stair",("top","-u")); P(0,122,b,"pane"); P(0,122,c,"stair",("top","+u"))
    P(0,121,-1,"stair",("top","-u")); P(0,121,1,"stair",("top","+u"))
    for u in (-1,0,1): P(0,122,u,"full","spruce_planks")
    for u in (-4,-3,-2,-1,1,2,3,4):
        if u in (-1,1): continue
        P(1,119,u,"full","spruce_planks")
    for y,us in ((119,(-1,1)),(120,(-1,1)),(121,(-1,0,1)),(122,(-1,0,1))):
        for u in us: P(1,y,u,"disc" if disc_stripped else "full","spruce_planks")
    for y in (120,121,122):
        for u in (-4,-3,-2,2,3,4): P(1,y,u,"full",wool)
    for d in (0,1):
        P(d,123,-5,"log_v"); P(d,123,5,"log_v")
        for u in range(-4,5): P(d,123,u,"log_u")
    for u in range(-5,6):
        P(0,124,u,"stair",("bottom","+d"))
        if u==kamin_u: P(1,124,u,"full","cobblestone")
        else: P(1,124,u,"stair",("bottom","-d"))
    P(1,125,kamin_u,"full","cobblestone"); P(1,126,kamin_u,"campfire")
    # Vordach + Laternen
    for u in (-1,0,1): P(-1,122,u,"slab","top")
    for u in (-1,1): P(-1,121,u,"lantern_hang")
    # Kellerhals d2..d6
    for d in (2,3,4,5):
        for u in (-2,2):
            P(d,119,u,"full","spruce_planks")
            P(d,120,u,"pane" if d==4 else "full","spruce_planks")
            P(d,121,u,"stair",("bottom","+u" if u<0 else "-u"))
        for u in (-1,1): P(d,122,u,"stair",("bottom","+u" if u<0 else "-u"))
    for y,row in ((119,{-2:"log_v",-1:"p",0:"p",1:"p",2:"log_v"}),(120,{-2:"log_v",-1:"p",0:"p",1:"p",2:"log_v"}),
                  (121,{-2:"s+",-1:"p",0:"p",1:"p",2:"s-"}),(122,{-1:"s+",0:"p",1:"s-"})):
        for u,k in row.items():
            if k=="p": P(6,y,u,"full","spruce_planks")
            elif k=="log_v": P(6,y,u,"log_v")
            else: P(6,y,u,"stair",("bottom","+u" if k=="s+" else "-u"))
    for d in (2,3,4,5,6): P(d,123,0,"slab","bottom")
    P(2,122,0,"lantern_hang"); P(5,122,0,"lantern_hang")
    # Treppe hinab d3..d7
    for i,d in enumerate((3,4,5,6,7)):
        for u in (-1,0,1): P(d,117-i,u,"stair",("bottom","-d"))
    P(7,115,-1,"torch_wall")
    # Tuer, Garten
    P(1,119,0,"door")
    for u in range(-5,6):
        P(-3,119,u,"gate" if u==0 else "fence")
    for d in (-2,-1):
        for u in (-5,5): P(d,119,u,"fence")
    for u in (-1,1): P(-3,120,u,"lantern_top")
    return B
dig=[]   # lokale Abbau-Liste
def digs():
    D=[]
    for d in (-3,-2,-1,0): D.append((d,118,0))
    D.append((1,118,0))
    for u in (-1,0,1): D.append((2,118,u))
    for d,ys in ((3,(118,117)),(4,(118,117,116)),(5,(118,117,116,115)),(6,(117,116,115,114)),(7,(116,115,114,113))):
        for y in ys:
            for u in (-1,0,1): D.append((d,y,u))
    return D

FACE={(1,0):"east",(-1,0):"west",(0,1):"south",(0,-1):"north"}
YAW={(1,0):-90.0,(-1,0):90.0,(0,1):0.0,(0,-1):180.0}
NONSOLID=("air","leaves","short_grass","tall_grass","dandelion","poppy","cornflower","azure_bluet","oxeye_daisy","bush","torch","fern","sapling","flower")
def build(name, x0, z0, dv, uv, wool, stripped, kamin_u, scanfile, extra_breaks=()):
    W={}
    for l in open(scanfile):
        p=l.split()
        if len(p)>=4: W[(int(p[0]),int(p[1]),int(p[2]))]=p[3].split('[')[0]
    def w(d,y,u): return (x0+d*dv[0]+u*uv[0], y, z0+d*dv[1]+u*uv[1])
    vec={"+d":dv,"-d":(-dv[0],-dv[1]),"+u":uv,"-u":(-uv[0],-uv[1])}
    B=spec(wool,stripped,kamin_u)
    D=[w(*t) for t in digs()]
    solid=set(k for k,n in W.items() if not any(s in n for s in NONSOLID))
    for p in D: solid.discard(p)
    for p in extra_breaks: solid.discard(p)
    M=lambda i:"minecraft:"+i
    def hit(face,hy):
        return {"up":[0.5,1.0,0.5],"down":[0.5,0.0,0.5],"east":[1.0,hy,0.5],"west":[0.0,hy,0.5],"south":[0.5,hy,1.0],"north":[0.5,hy,0.0]}[face]
    def clicks(pos,kind,arg):
        x,y,z=pos; out=[]
        below=(x,y-1,z); above=(x,y+1,z)
        sides=[((x-a,y,z-b),FACE[(a,b)]) for (a,b) in FACE]
        if kind=="log_u":
            ux,uz=uv
            return [((x-ux,y,z-uz),FACE[(ux,uz)],0.5),((x+ux,y,z+uz),FACE[(-ux,-uz)],0.5)]
        if kind=="lantern_hang": return [(above,"down",0)]
        if kind=="torch_wall":
            ux,uz=uv; return [((x-ux,y,z-uz),FACE[(ux,uz)],0.5)]
        top = (kind=="stair" and arg[0]=="top") or (kind=="slab" and arg=="top")
        if top: return [(above,"down",0)]+[(n,f,0.75) for n,f in sides]
        if kind in("stair","slab"): return [(below,"up",0)]+[(n,f,0.25) for n,f in sides]
        if kind in("log_v",): return [(below,"up",0),(above,"down",0)]
        return [(below,"up",0)]+[(n,f,0.5) for n,f in sides]
    item={"full":None,"pane":"glass_pane","gravel":"gravel","log_v":"spruce_log","log_u":"spruce_log","disc":"spruce_log",
          "stair":"spruce_stairs","slab":"spruce_slab","lantern_hang":"lantern","lantern_top":"lantern","campfire":"campfire",
          "door":"spruce_door","fence":"spruce_fence","gate":"spruce_fence_gate","torch_wall":"torch"}
    def step_for(b, chosen):
        d,y,u,kind,arg=b; pos=w(d,y,u); n,face,hy=chosen
        it=arg if kind=="full" else item[kind]
        st={"action":"place","item":M(it),"pos":list(pos),"click":list(n),"face":face,"hit":hit(face,hy)}
        if kind=="stair": st["yaw"]=YAW[vec[arg[1]]]
        if kind in("door","gate"): st["yaw"]=YAW[dv]
        return st
    counts=collections.Counter()
    for b in B:
        k=b[3]; counts[b[4] if k=="full" else item[k]]+=1
    return W,w,B,D,solid,clicks,step_for,counts
