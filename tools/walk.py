import sys,collections
b={}
for f in ('scan/halle.txt','scan/sued.txt'):
  for l in open(f):
    p=l.split()
    if len(p)>=4: b[(int(p[0]),int(p[1]),int(p[2]))]=p[3].split('[')[0]
SOFT=('air','short_grass','torch','rail','powered_rail','tall_grass','wall_torch')
def g(x,y,z): return b.get((x,y,z),'air')
def ok(x,z): return g(x,119,z) in SOFT and g(x,120,z) in SOFT and g(x,118,z) not in SOFT
def path(s,t):
    prev={s:None};q=collections.deque([s])
    while q:
        c=q.popleft()
        if c==t: break
        for dx,dz in ((1,0),(-1,0),(0,1),(0,-1)):
            n=(c[0]+dx,c[1]+dz)
            if n not in prev and -1475<=n[0]<=-1390 and 985<=n[1]<=1060 and ok(*n):
                prev[n]=c;q.append(n)
    if t not in prev: return None
    p=[t]
    while prev[p[-1]]: p.append(prev[p[-1]])
    p=p[::-1]
    # simplify: keep corners
    out=[p[0]]
    for i in range(1,len(p)-1):
        a,bb,c=p[i-1],p[i],p[i+1]
        if (bb[0]-a[0],bb[1]-a[1])!=(c[0]-bb[0],c[1]-bb[1]): out.append(bb)
    out.append(p[-1]); return out
if __name__=="__main__":
    s=tuple(map(int,sys.argv[1].split(',')));t=tuple(map(int,sys.argv[2].split(',')))
    print(path(s,t))
