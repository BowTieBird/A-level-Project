class RAM extends Hardware {  
  int m = min(int(W/100), 10);
  int n = m*m;
  int len = MEMORY_LENGTH;
  
  float memOffset = 5;
  float memXPadding = 10, memYPadding = 19;
  float memWidth = len * 10 - 1, memHeight = 20;
  float memLabelOffset = -5;
  
  float RAMWidth = m*(memWidth + memXPadding);
  float RAMHeight = m*(memHeight + memYPadding);
  float startX = W-RAMWidth-70, startY = 10;
  float labelHeight = 20;
  float labelSize = 15;
  
  color bg = color(17, 72, 166);
  
  StartTag startTagSpawner = new StartTag(this);
  
  MemoryAddress[] memory = new MemoryAddress[n];
  
  MemoryAddress getRegister(int k) {
    return memory[k % n];
  }
  
  RAM() {
    setBackground(bg);
    setSize(RAMWidth, RAMHeight + labelHeight);
    setPos(startX, startY);
    
    setUpStartTagSpawner();
    setUpLabel();    
    setUpRegisters();
    updatePos();
  }
  
  void setUpStartTagSpawner() {
    startTagSpawner.setOffsetX(-startTagSpawner.getTotalWidth());
  }
  
  void setUpLabel() {
    setLabel("RAM");
    setLabelOffset(labelSize);
    setLabelSize(labelSize);
  }
  
  void setUpRegisters() {
    for (int k = 0; k < memory.length; k++) {
      float registerX = memOffset + (k % m)*(memWidth + memXPadding);
      float registerY = memOffset + (k / m)*(memHeight + memYPadding);
      
      MemoryAddress current = new MemoryAddress(len);
      current.setParent(this);
      current.setSize(memWidth, memHeight);
      current.setOffset(registerX, registerY);
      current.setLabelOffset(memLabelOffset);
      current.setLabel(str(k));
      //current.updateLength();
      memory[k] = current;
    }
  }
  
  void updatePos() {
    for (MemoryAddress register : memory) register.updatePos();
    startTagSpawner.updatePos();
  }
  
  void show() {
    super.show();
    for (MemoryAddress register : memory) register.show();
    startTagSpawner.show();
  } 

  void drag() {
    super.drag();
    updatePos();
  }
  
  void mousePress() {
    if (startTagSpawner.mouseOver()) {
      Tag newStartTag = startTagSpawner.copy();
      newStartTag.drag();
      tags.addTag(newStartTag);
    } else super.mousePress();
  }
  
  void clearData() {
    for (MemoryAddress register : memory) register.setWillHaveData(false);
  }
  
  void assemble() {
    clearData();
    scripts.assemble();
  }
  
  int nextRegister() {
    for (int i = 0; i < n; i++) if (!memory[i].willHaveData()) return i;
    clearData();
    return 0;
  }
  
  void run() {
    for (MemoryAddress r : memory) r.run();
  }
  
  ArrayList<String> saveState() {
    ArrayList<String> states = new ArrayList<String>();
    states.add("#RAM");
    states.addAll(super.saveState());
    for (int k = 0; k < n; k++) states.addAll(memory[k].saveState(k));
    return states;
  }
  
  void loadState(ArrayList<String> states) {
    int index = states.indexOf("#RAM");
    ArrayList<String> state = new ArrayList<String>(states.subList(index + 1, index + 8));
    super.loadState(state);
    int k = 0;
    while (states.contains("#MemoryAddress " + k) && k < n) { 
      memory[k].loadState(states, k);
      k++;
    }
    setSize(RAMWidth, RAMHeight + labelHeight);
    updatePos();
  }
  
  void helpOverlay() {
    show();
    fill(255);
    textSize(20);
    textAlign(CENTER, TOP);
    text("^ The RAM is where the program is stored.", cx, y + h);
    text("Press the blue compile arrow to load the program into the RAM.", cx, y + h + textAscent());
    textAlign(CENTER, TOP);
    text("A script with this tag will be the first script loaded into the RAM. ->", startTagSpawner.getX(), startTagSpawner.getCenterY(), -250, 500);
  }
}

class MemoryAddress extends FTextBox {
  boolean willHaveData = false;
  boolean willHaveData() { return willHaveData; }
  void setWillHaveData(boolean b) { willHaveData = b; }
  
  MemoryAddress(int n) { super(n); }
  
  color ramParticleColor = color(10, 0, 200);
  
  ParticleAttractor assemble = new ParticleAttractor(this);
  ParticleAttractor fetchInstruction = new ParticleAttractor(this);
  ParticleAttractor fetchData = new ParticleAttractor(this);
  ParticleAttractor store = new ParticleAttractor(this);
  
  void assemble(Particle p) {
    assemble.addParticle(p);
  }
  
  void fetchInstruction(Particle p) {
    fetchInstruction.addParticle(p);
  }
  
  void returnInstruction() {
    cpu.returnInstruction(setUpNewParticle());
  }
  
  void fetchData(Particle p) {
    fetchData.addParticle(p);
  }
  
  void returnData() {
    cpu.returnData(setUpNewParticle());
  }
  
  void store(Particle p) {
    store.addParticle(p);
  }
  
  void store(int s) {
    setText(s);
    pc.step();
  }
  
  void run() {
    if (assemble.gotInput()) setText(assemble.input());
    if (fetchInstruction.gotInput()) returnInstruction();
    if (fetchData.gotInput()) returnData();
    if (store.gotInput()) store(store.input());
  }
  
  Particle setUpNewParticle() {
    Particle newParticle = new Particle(getText());
    newParticle.setPos(cx, cy);
    newParticle.setBackground(ramParticleColor);
    return newParticle;
  }
  
  ArrayList<String> saveState(int k) {
    ArrayList<String> states = new ArrayList<String>();
    states.add("#MemoryAddress " + k);
    states.add(getText());
    return states;
  }
  
  void loadState(ArrayList<String> states, int k) {
    int index = states.indexOf("#MemoryAddress " + k);
    setText(int(states.get(index + 1)));
    updatePos();
  }
}
