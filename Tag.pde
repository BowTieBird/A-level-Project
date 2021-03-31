class Tag extends Typable {
  float tagHeight = 30;
  float pointerMult = 0.5;
  
  color bg = color(33, 183, 33);
  color mg = color(255);
  color hl = color(227, 232, 41);
  color particleColor = color(0, 202, 0);
  
  float textPadding = 3;
  
  float getTotalWidth() { return w + pointer.getWidth(); }
  
  Pointer pointer;
  
  Tag(Box p) {
    setParent(p);
    setBackground(bg);
    setTextColor(mg);
    setHeight(tagHeight);
    
    setMin(3);
    setPadding(textPadding);
    setTextOffset(textPadding);
    updateLength();
    
    pointer = new Pointer(this);
  }
  
  Tag copy() {
    Tag newTag = new Tag(null);
    newTag.setPos(x, y);
    newTag.setText(getText());
    newTag.updateWidth();
    return newTag;
  }
  
  void setPos(float x, float y) {
    super.setPos(x, y);
    pointer.updatePos();
  }
  
  void updateWidth() {
    updateLength();
    if (parent instanceof Block) {
      Block p = (Block)parent; 
      p.updateTag(this);
    }
    if (hasParent()) updatePos();
    pointer.setOffsetX(getWidth());
    pointer.updatePos();
  }
  
  void show() {
    pointer.show();
    super.show();
  }
  
  boolean mouseOver() {
    return (super.mouseOver() || pointer.mouseOver());
  }
  
  void release() {
    tags.select(this);
    super.release();
    tags.release(this);
  }
  
  boolean isNear(SnapPoint other) {
    return pointer.isNear(other);
  }
  
  void detach() {
    if (parent instanceof Block) {
      Block p = (Block)parent; 
      p.detach(this);
    }
    setParent(null);
  }
  
  int getRegisterOfTag() {
      if (parent instanceof Block) {
      Block b = (Block)parent;
      return b.getRegIndex();
    }
    return 0;
  }
  
  Particle setUpNewParticle() {
    Particle newParticle = new Particle(nf(getRegisterOfTag(), 2));
    newParticle.setPos(cx, cy);
    newParticle.setBackground(particleColor);
    newParticle.show();
    return newParticle;
  }
  
  ArrayList<String> saveState() {
    ArrayList<String> state = new ArrayList<String>();
    state.add("#EmptyTag");
    state.add(str(getX()));
    state.add(str(getY()));
    return state;
  }
}

class StartTag extends Tag {
  StartTag(Box parent) {
    super(parent);
  }
  
  void show() {
    super.show();
    flag();
  }
  
  void flag() {
    stroke(10);
    fill(particleColor);
    float dw = 8;
    float dz = 5;
    float p = 18, q = 12;
    float theta = 0.1;
    pushMatrix();
    translate(cx - dw, cy);
    rotate(theta);
    line(0,h/2-dz,0,dz-h/2);
    rect(0,dz-h/2,p, q);
    popMatrix();
    noStroke();
  }
  
  void release() {
    super.release();
    deSelect();
  }
  
  Tag copy() {
    StartTag newTag = new StartTag(null);
    newTag.setPos(x, y);
    return newTag;
  }
  
  Script getStartingScript() {
    Block block = (Block)parent;
    if (block == null) return null;
    else return block.getScript();
  }
}