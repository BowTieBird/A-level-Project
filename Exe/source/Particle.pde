class Particle extends Colored {
  color FG = color(255);

  float R = 15;
  float TEXT_SIZE = 16;

  float GRAVITY_FACTOR = 0.1;
  float DRAG_FACTOR = 0.01;
  float BEETLING_FACTOR = .1; //Optional for beetling eg .4
  
  float v0 = random(10);
  float t = 0;
  float dt = random(3);

  PVector pos = new PVector();
  PVector vel = PVector.mult(PVector.random2D(), v0 * sqrt(particles.getParticleSpeed()));
  PVector acc = new PVector();

  ParticleAttractor target;
  void setTarget(ParticleAttractor p) { target = p; }

  String data = "";
  String getData() { return data; }
  int getIntData() { return int(data); }

  Particle(String d) {
    setData(d);
  }
  
  void setData(String d) {
    data = d;
  }

  void setPos(float x, float y) {
    pos.set(x, y);
  }

  void update() {
    applyForce(targetGravity());
    applyForce(beetling());
    applyForce(drag());

    vel.add(acc);
    pos.add(vel);
    acc.mult(0); 
    
    t += dt;
  }
  
  void applyForce(PVector force) {
    acc.add(force);
    if (cheekyDevMode) devForce(this, force);
  }

  PVector targetGravity() {
    PVector targetGravity = target.targetGravity().sub(pos).setMag(sin(radians(t)) + 2);  
    return targetGravity.mult(GRAVITY_FACTOR * particles.getParticleSpeed());
  }

  PVector beetling() {
    return PVector.random2D().mult(BEETLING_FACTOR * sqrt(particles.getParticleSpeed()));
  }

  PVector drag() {
    return PVector.mult(vel, -vel.mag() * DRAG_FACTOR);
  }

  void show() {
    fill(bg);
    ellipseMode(RADIUS);
    ellipse(pos.x, pos.y, R, R);
    showText();

    if (cheekyDevMode) devReticle(this);
  }

  void showText() {
    fill(FG);
    textSize(TEXT_SIZE);
    textAlign(CENTER, CENTER);
    text(data, pos.x, pos.y);
  }
  
  boolean arrived() {
    return control.maxParticleSpeed() || target.distance(this) <= R*constrain(targetGravity().mag(), 1, 2);
  }
  
  void add(int b) {
    data = nf(getIntData() + b, data.length());
  }
}
