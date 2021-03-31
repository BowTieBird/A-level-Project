class CPU extends Hardware {
  color bg = color(230, 230, 10);
  color fg = color(205, 46, 0, 180);
  float startX = 225, startY = H-270;
  float startWidth = 180, startHeight = 190;

  float registerHeight = 25;
  float registerOffset = 30;
  float registerShelfHeight = 50;
  float registerLabelSize = 10;
  float registerLabelOffset = 55;

  float labelSize = 20;
  float labelOffset = 20;

  ArrayList<TextBox> registers = new ArrayList<TextBox>();
  TextBox CIR = new FTextBox(1);
  TextBox MAR = new FTextBox(2);
  TextBox MDR = new FTextBox(MEMORY_LENGTH);
  int getCIRValue() { return CIR.getIntText(); }

  color dataColor = color(254, 3, 234);
  color addressColor = color(245, 10, 10);
  color instructionColor = color(201, 8, 106);

  boolean instructionLoaded = false;

  ParticleAttractor returnCount = new ParticleAttractor(MAR);
  ParticleAttractor returnInstruction = new ParticleAttractor(MDR);
  ParticleAttractor loadAddress = new ParticleAttractor(MAR);
  ParticleAttractor loadInstruction = new ParticleAttractor(CIR);
  ParticleAttractor returnData = new ParticleAttractor(MDR);
  ParticleAttractor checkForBranch = new ParticleAttractor(MDR);

  CPU() {
    setBackground(bg);
    setSize(startWidth, startHeight);
    setPos(startX, startY);

    setUpRegisters();

    setUpLabel();
    updatePos();
  }

  void setUpRegisters() {
    setUpCIR();
    setUpMAR();
    setUpMDR();
    float registerY = -registerShelfHeight;
    for (TextBox register : registers) {
      register.setParent(this);
      register.setHeight(registerHeight);
      register.updateLength();
      register.setOffset(registerOffset, registerY);
      register.setLabelLeftAlign(LEFT);
      register.setLabelOffset(registerLabelOffset);
      register.setLabelSize(registerLabelSize);
      register.setLabelColor(fg);
      registerY += registerShelfHeight;
    }
  }

  void setUpCIR() {
    CIR.setLabel("Current" + "\n" + "Instruction" + "\n" + "Register");
    registers.add(CIR);
  }

  void setUpMAR() {
    MAR.setLabel("Memory Address" + "\n" + "Register");
    registers.add(MAR);
  }

  void setUpMDR() {
    MDR.setLabel("Memory Data" + "\n" + "Register");
    registers.add(MDR);
  }

  void setUpLabel() {
    setLabel("CPU");
    setLabelTopAlign(TOP);
    setLabelSize(labelSize);
    setLabelOffset(labelOffset);
    setLabelColor(fg);
  }

  void show() {
    super.show();
    for (TextBox register : registers) register.show();
  }

  void drag() {
    super.drag();
    updatePos();
  }

  void updatePos() {
    for (TextBox register : registers) {
      register.setX(cx);
      register.setCenterY(cy + labelOffset);
    }
  }

  void release() {
    select();
    super.release();
  }

  void returnCount(Particle p) {
    returnCount.addParticle(p);
  }

  void returnInstruction(Particle p) {
    returnInstruction.addParticle(p);
  }

  void returnData(Particle p) {
    returnData.addParticle(p);
  }

  void checkForBranch(Particle p) {
    checkForBranch.addParticle(p);
  }

  void fetchInstruction(int c) {
    MAR.setText(c);
    ram.getRegister(c).fetchInstruction(setUpParticle(MAR, addressColor));
  }

  void decode(int d) {
    MDR.setText(d);
    loadAddress.addParticle(setUpParticleWithData(MDR, addressColor, nf(d % 100, 2)));
    loadInstruction.addParticle(setUpParticleWithData(MDR, instructionColor,  nf(d / 100, 1)));
  }

  void loadAddress(int d) {
    MAR.setText(d);
    checkForExecute();
  }

  void loadInstruction(int d) {
    CIR.setText(d);
    checkForExecute();
  }

  void checkForExecute() {
    if (instructionLoaded) execute();
    instructionLoaded ^= true;
  }

  void execute() {
    int instruction = CIR.getIntText();
    switch (instruction) {
    case 1: //ADD
    case 2: //SUBTRACT
    case 5: //LOAD ADDRESS
      fetchData();
      break;
    case 3: //STORE
      store();
      break;
    case 6: //BRANCH
      branch();
      break;
    case 7: //BRANCH IF ZERO
    case 8: //BRANCH IF POSITIVE
      acc.checkForBranch();
      break;
    case 9:
      int address = MAR.getIntText();
      switch (address) {
      case 01:
        input();
        break;
      case 02:
        output();
        break;        
      }
      break;
    default: //HALT
      break;
    }
  }
  
  void fetchData() {
    MemoryAddress address = ram.getRegister(MAR.getIntText());
    address.fetchData(setUpParticle(MAR, addressColor));
  }
  
  void calc() {
    acc.calcAcc();
    alu.calcData(setUpParticle(MDR, dataColor));
  }
  
  void load() {
    acc.load(setUpParticle(MDR, dataColor));
  }
  
  void store() {
    acc.store(setUpParticle(MAR, addressColor));
  }
  
  void branch() {
    pc.branch(setUpParticle(MAR, addressColor));
  }
  
  void checkForBranch(int b) {
    MDR.setText(b);
    int instruction = CIR.getIntText();
    if (instruction == 7) branchIf(b == 0);
    if (instruction == 8) branchIf(b > 0);
  }
  
  void branchIf(boolean condition) {
    if (condition) branch();
    else pc.step();
  }
  
  void input() {
    inp.input(setUpParticleWithData(CIR, instructionColor, "INP"));
  }
  
  void output() {
    acc.output();
  }

  void run() {
    if (returnCount.gotInput()) fetchInstruction(returnCount.input());
    if (returnInstruction.gotInput()) decode(returnInstruction.input());
    if (loadAddress.gotInput()) loadAddress(loadAddress.input());
    if (loadInstruction.gotInput()) loadInstruction(loadInstruction.input());
    if (returnData.gotInput()) {
      MDR.setText(returnData.input());
      if (getCIRValue() == 5) load();
      else calc();
    }
    if (checkForBranch.gotInput()) checkForBranch(checkForBranch.input());
  }

  Particle setUpParticle(TextBox r, color c) {
    Particle newParticle = new Particle(r.getText());
    newParticle.setPos(r.getCenterX(), r.getCenterY());
    newParticle.setBackground(c);
    return newParticle;
  }
  
  Particle setUpParticleWithData(TextBox r, color c, String d) {
    Particle newParticle = setUpParticle(r, c);
    newParticle.setData(d);
    return newParticle;
  }
  
  ArrayList<String> saveState() {
    ArrayList<String> states = new ArrayList<String>();
    states.add("#CPU");
    states.addAll(super.saveState());
    for (TextBox register : registers) states.add(register.getText());
    return states;
  }
  
  void loadState(ArrayList<String> states) {
    int index = states.indexOf("#CPU");
    ArrayList<String> state = new ArrayList<String>(states.subList(index + 1, index + 10));
    super.loadState(state);
    CIR.setText(state.get(6));
    MAR.setText(state.get(7));
    MDR.setText(int(state.get(8)));
    updatePos();
  }
  
  void helpOverlay() {
    show();
    fill(255);
    textSize(20);
    textAlign(CENTER, BOTTOM);
    text("\\/", cx, y);
    text("The CPU is the brains of the computer.", cx, y - textAscent());
    textAlign(LEFT, CENTER);
    text(" <- The CIR stores the current instruction.", x + w, CIR.getCenterY());
    text(" <- The MAR stores the address of a register in the RAM.", x + w, MAR.getCenterY());
    text(" <- The MDR stores the data from the address.", x + w, MDR.getCenterY());
  }
}