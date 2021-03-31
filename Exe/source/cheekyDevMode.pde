void devMode() {
  cheekyDevMode ^= true;
}
void devFrame() {
  devForces();
  devText();
}
void devDraw(Box s) {
  if (s.mouseOver()) { 
    fill(0); 
    rect(s.x, s.y, 6, 6);
  }
  stroke(0);
  float r = min(s.w, s.h)/5;
  line(s.x, s.y, s.x+r, s.y      );
  line(s.x, s.y, s.x, s.y+r    );
  line(s.x+s.w, s.y, s.x+s.w-r, s.y      );
  line(s.x+s.w, s.y, s.x+s.w, s.y+r    );
  line(s.x, s.y+s.h, s.x+r, s.y+s.h  );
  line(s.x, s.y+s.h, s.x, s.y+s.h-r);
  line(s.x+s.w, s.y+s.h, s.x+s.w-r, s.y+s.h  );
  line(s.x+s.w, s.y+s.h, s.x+s.w, s.y+s.h-r);
  line(s.cx-r, s.cy, s.cx+r, s.cy  );
  line(s.cx, s.cy-r, s.cx, s.cy+r);
  noStroke();
}

void devSnap(SnapPoint c) {
  fill(205, 100);
  ellipseMode(CENTER);
  ellipse(c.cx, c.cy, c.snapRadius, c.snapRadius);
}

void devReticle(Particle p) {
  stroke(p.bg);
  noFill();
  //ellipse(p.target.target.getCenterX(), p.target.target.getCenterY(), p.r, p.r);
  noStroke();
}

float forceMultiplier = 150;

ArrayList<Particle> pForce = new ArrayList<Particle>();
ArrayList<PVector> fForce = new ArrayList<PVector>();

void devForce(Particle p, PVector f) {
  pForce.add(p);
  fForce.add(f);
}

void devForces() {
  stroke(color(255, 255, 0));
  for (int i = 0; i < fForce.size(); i++) {
    Particle p = pForce.get(i);
    PVector f = fForce.get(i);
    PVector fmult = f.copy().mult(forceMultiplier);
    float arrowHeadSize = fmult.mag()/5;
    PVector pos1 = p.pos.copy();
    PVector pos2 = PVector.add(pos1, fmult);
    float angle = atan2(fmult.y, fmult.x);
    line(pos1.x, pos1.y, pos2.x, pos2.y);
    line(pos2.x, pos2.y, (pos2.x+cos(angle+PI*0.75)*arrowHeadSize), (pos2.y+sin(angle+PI*0.75)*arrowHeadSize));
    line(pos2.x, pos2.y, (pos2.x+cos(angle+PI*1.25)*arrowHeadSize), (pos2.y+sin(angle+PI*1.25)*arrowHeadSize));
  }
  noStroke();
  pForce.clear();
  fForce.clear();
}

float devColumnWidth = 40;
float devRowHeight = 15;
int devRow = 0;

void devText() {
  fill(200);
  textSize(devRowHeight);
  textAlign(RIGHT, DOWN);
  devRow = 0;
  devText("fps:", str((int)frameRate));
  devText("scripts size:", str(scripts.scripts.size()));
  devText("tags size:", str(tags.tags.size()));
}

void devText(String label, String obj) {
  text(label, W - devColumnWidth, H - devRowHeight * devRow);
  text(obj, W, H - devRowHeight * devRow);
  devRow++;
}
