class ProgramCounter extends Hardware {
  color bg = color(250, 148, 1);
  color countColor = color(250, 50, 220);
  float startX = 420, startY = H-160;
  float startWidth = 55, startHeight = 60;

  float pcHeight = 23;
  float pcOffset = 15;
  float labelOffset = 18;
  
  FTextBox pc = new FTextBox(2);
  
  ParticleAttractor incrementPC = new ParticleAttractor(pc);
  ParticleAttractor branch = new ParticleAttractor(pc);

  ProgramCounter() {
    setBackground(bg);
    setSize(startWidth, startHeight);
    setPos(startX, startY);

    setUpLabel();
    setUpPC();
    updatePos();
  }
  
  void setUpLabel() {
    setLabel("Program"+"\n"+"Counter");
    setLabelOffset(labelOffset);
  }

  void setUpPC() {
    pc.setParent(this);
    pc.setOffsetY(pcOffset);
    pc.setHeight(pcHeight);
    pc.updateLength();
  }
  
  void show() {
    super.show();
    pc.show();
  }
  
  void drag() {
    super.drag();
    updatePos();
  }
  
  void updatePos() {
    pc.setCenter(cx, y);
  }
  
  Particle setUpNewParticle() {
    Particle newParticle = new Particle(pc.getText());
    newParticle.setPos(cx, cy);
    newParticle.setBackground(bg);
    return newParticle;
  }
  
  void step() {
    returnCount();
    incrementPC();
  }
  
  void returnCount() {
    cpu.returnCount(setUpNewParticle());
  }
  
  void incrementPC() {
    alu.incrementPC(setUpNewParticle());
  }
  
  void returnIncrement(Particle p) {
    incrementPC.addParticle(p);
  }
  
  void branch(Particle p) {
    branch.addParticle(p);
  }
  
  void branch(int b) {
    pc.setText(b);
    step();
  }
  
  void run() {
    if (incrementPC.gotInput()) pc.setText(incrementPC.input());
    if (branch.gotInput()) branch(branch.input());
  }
  
  void reset() {
    pc.setText(0);
  }
  
  ArrayList<String> saveState() {
    ArrayList<String> state = new ArrayList<String>();
    state.add("#PC");
    state.addAll(super.saveState());
    state.add(pc.getText());
    return state;
  }
  
  void loadState(ArrayList<String> states) {
    int index = states.indexOf("#PC");
    ArrayList<String> state = new ArrayList<String>(states.subList(index + 1, index + 8));
    super.loadState(state);
    pc.setText(state.get(6));
    updatePos();
  }
  
  void helpOverlay() {
    show();
    fill(255);
    textSize(20);
    textAlign(CENTER, TOP);
    text(" <- The Program Counter stores the address of the current instruction.", x + w, y, 500, 100);
  }
}

class Accumulator extends Hardware {
  color bg = color(240, 23, 89);
  float startX = 300, startY = H-70;
  float startWidth = max(100, (MEMORY_LENGTH)*15), startHeight = 60;

  float accHeight = 25;
  float accOffset = 20;
  float labelOffset = 15;
  
  FTextBox acc = new FTextBox(MEMORY_LENGTH);
  
  ParticleAttractor load = new ParticleAttractor(acc);
  ParticleAttractor store = new ParticleAttractor(acc);
  ParticleAttractor input = new ParticleAttractor(acc);

  Accumulator() {
    setBackground(bg);
    setSize(startWidth, startHeight);
    setPos(startX, startY);
    
    setUpLabel();
    setUpAcc();
    updatePos();
  }
  
  void setUpLabel() {
    setLabel("Accumulator");
    setLabelOffset(labelOffset);
  }

  void setUpAcc() {
    acc.setParent(this);
    acc.setHeight(accHeight);
    acc.setOffsetY(accOffset);
    acc.updateLength();
  }
  
  void show() {
    super.show();
    acc.show();
  }
  
  void drag() {
    super.drag();
    updatePos();
  }
  
  void updatePos() {
    acc.setCenter(cx, y);
  }
  
  void calcAcc() {
    alu.calcAcc(setUpNewParticle());
  }
  
  void load(Particle particle) {
    load.addParticle(particle);
  }
  
  void load() {
    acc.setText(load.input());
    pc.step();
  }  
  
  void store(Particle particle) {
    store.addParticle(particle);
  }
  
  void store(int s) {
    MemoryAddress r = ram.getRegister(s);
    r.store(setUpNewParticle());
  }
  
  void checkForBranch() {
    cpu.checkForBranch(setUpNewParticle());
  }
  
  void run() {
    if (load.gotInput()) load();
    if (store.gotInput()) store(store.input());
  }
  
  void reset() {
    acc.setText(0);
  }
  
  void output() {
    out.output(setUpNewParticle());
    pc.step();
  }
  
  Particle setUpNewParticle() {
    Particle newParticle = new Particle(acc.getText());
    newParticle.setPos(cx, cy);
    newParticle.setBackground(bg);
    return newParticle;
  }
  
  ArrayList<String> saveState() {
    ArrayList<String> state = new ArrayList<String>();
    state.add("#Acc");
    state.addAll(super.saveState());
    state.add(acc.getText());
    return state;
  }
  
  void loadState(ArrayList<String> states) {
    int index = states.indexOf("#Acc");
    ArrayList<String> state = new ArrayList<String>(states.subList(index + 1, index + 8));
    super.loadState(state);
    acc.setText(int(state.get(6)));
    updatePos();
  }
  
  void helpOverlay() {
    show();
    fill(255);
    textSize(20);
    textAlign(CENTER, BOTTOM);
    text("\\/", cx, y);
    text("The Accumulator stores a value.", cx, y - 3*textAscent());
    text("ADD or SUB Blocks change the value.", cx, y - 2*textAscent());
    text("Branch Blocks check the value.", cx, y - textAscent());
  }
}

class ALU extends Hardware {
  color bg = color(163, 1, 163);
  float startX = 415, startY = H-90;
  float startWidth = 120, startHeight = 80;
  
  float labelSize = 15;
   
  ParticleAttractor incrementPC = new ParticleAttractor(this);
  ParticleAttractor calcAcc = new ParticleAttractor(this);
  ParticleAttractor calcData = new ParticleAttractor(this);
  
  ALU() {
    setBackground(bg);
    setSize(startWidth, startHeight);
    setPos(startX, startY);
    setUpLabel();
  }
  
  void setUpLabel() {
    setLabel("Arithmetic" + "\n" + "Logic" + "\n" + "Unit");
    setLabelTopAlign(CENTER);
    setLabelSize(labelSize);
  }
  
  void incrementPC(Particle particle) {
    incrementPC.addParticle(particle);
  }
  
  void calcAcc(Particle particle) {
    calcAcc.addParticle(particle);
  }  

  void calcData(Particle particle) {
    calcData.addParticle(particle);
  }
  
  void run() {
    Particle PCParticle = incrementPC.getInput();
    if (PCParticle != null) {
      PCParticle.setData(calculate(PCParticle, 1, 2));
      incrementPC.removeParticle(PCParticle);
      pc.returnIncrement(PCParticle);
    }
    Particle AccParticle = calcAcc.getInput();
    if (AccParticle != null) {
      if (calcData.gotInput()) {
        int data = 0;
        int instruction = cpu.getCIRValue();
        if (instruction == 1) data = calcData.input();  
        if (instruction == 2) data = -calcData.input();
        AccParticle.setData(calculate(AccParticle, data, MEMORY_LENGTH));
        calcAcc.removeParticle(AccParticle);
        acc.load(AccParticle);
      }
    }
  }
  
  String calculate(Particle p, int a, int mod) {
    return nf((p.getIntData() + a) % int(pow(10, mod)), mod);
  }
  
  ArrayList<String> saveState() {
    ArrayList<String> state = new ArrayList<String>();
    state.add("#ALU");
    state.addAll(super.saveState());
    return state;
  }
  
  void loadState(ArrayList<String> states) {
    int index = states.indexOf("#ALU");
    ArrayList<String> state = new ArrayList<String>(states.subList(index + 1, index + 7));
    super.loadState(state);
  }
  
  void helpOverlay() {
    show();
    fill(255);
    textSize(20);
    textAlign(CENTER, CENTER);
    text(" <- This unit deals with adding, subtracting and other logic.", x + w, y, 400, 100);
  }
}

class Input extends Hardware {
  color bg = color(92, 255, 147);
  float startX = 260, startY =  60;
  float startWidth = 150, startHeight = 80;

  float labelOffset = 20;
  float labelSize = 20;

  float inputY = 15;
  float inputWidth = 100;
  float inputHeight = 30;
  int inputLength = 8;
  
  boolean active = false;

  Typable inp = new Typable();

  ParticleAttractor input = new ParticleAttractor(inp);

  Input() {
    setBackground(bg);
    setSize(startWidth, startHeight);
    setPos(startX, startY);
    
    setUpLabel();
    setUpInput();
    updatePos();
  }
  
  void setUpLabel() {
    setLabel("Input");
    setLabelSize(labelSize);
    setLabelOffset(labelOffset);
  }

  void setUpInput() {
    inp.setParent(this);
    inp.setOffsetY(inputY);
    inp.setSize(inputWidth, inputHeight);
    inp.setLimits(inputLength, inputLength);
    inp.updateLength();
  }

  void show() {
    super.show();
    inp.show();
  }

  void drag() {
    super.drag();
    updatePos();
  }
  
  void release() {
    super.release();
    if (active) select();
  }

  void updatePos() {
    inp.setCenterX(cx);
    inp.setY(y);
  }

  void select() {
    inp.select();
    super.select();
  }
  
  void deSelect() {
    inp.deSelect();
    super.deSelect();
  }
  
  void input(Particle particle) {
    input.addParticle(particle);
  }
  
  void run() {
    if (input.gotInput()) activate();
  }
  
  void activate() {
    active = true;
    select();
  }
  
  void input() {
    acc.load(setUpNewParticle());
    active = false;
  }
  
  Particle setUpNewParticle() {
    Particle newParticle = new Particle(inp.getText());
    newParticle.setPos(cx, cy);
    newParticle.setBackground(bg);
    return newParticle;
  }
  
  ArrayList<String> saveState() {
    ArrayList<String> state = new ArrayList<String>();
    state.add("#Input");
    state.addAll(super.saveState());
    state.add(inp.getText());
    return state;
  }
  
  void loadState(ArrayList<String> states) {
    int index = states.indexOf("#Input");
    ArrayList<String> state = new ArrayList<String>(states.subList(index + 1, index + 8));
    super.loadState(state);
    inp.setText(state.get(6));
    updatePos();
  }
  
  void helpOverlay() {
    show();
    fill(255);
    textSize(20);
    textAlign(CENTER, TOP);
    text("^", cx, y + h);
    text("The Input Block lets you input a value into your program.", x - 50, y + h + textAscent(), w + 100, 300);
  }
}

class Output extends Hardware {
  color bg = color(210, 71, 121);
  float startWidth = 180, startHeight = 200;
  float startX = W-startWidth-80, startY = H-210;

  float outputWidth = 160;
  float outputHeight = 30;
  float outputOffset = 40;
  
  float labelSize = 20;
  float labelOffset = 18;

  int n = 5;
  TextBox[] outs = new TextBox[n];
  ParticleAttractor[] outputs = new ParticleAttractor[n];

  Output() {
    setBackground(bg);
    setSize(startWidth, startHeight);
    setPos(startX, startY);

    setUpLabel();
    setUpOutputs();
    updatePos();
  }
  
  void setUpLabel() {
    setLabel("Output");
    setLabelTopAlign(TOP);
    setLabelSize(labelSize);
    setLabelOffset(labelOffset);
  }

  void setUpOutputs() {
    for (int i = 0; i < n; i++) {
      TextBox output = new TextBox();
      output.setParent(this);
      output.setOffsetY(outputOffset);
      output.setSize(outputWidth, outputHeight);
      output.setMin(MEMORY_LENGTH);
      outputOffset += outputHeight;
      outputs[i] = new ParticleAttractor(output); 
      outs[i] = output;
    }
  }

  void show() {
    super.show();
    for (TextBox output : outs) output.show();
  }

  void drag() {
    super.drag();
    updatePos();
  }

  void updatePos() {
    for (TextBox output : outs) {
      output.setCenterX(cx);
      output.setY(y);
    }
  }
  
  void output(Particle particle) {
    for (int i = 0; i < n; i++) if (i == n - 1 || outs[i].getText() == "") {
      outputs[i].addParticle(particle);
      break;
    }
  }
  
  void run() {
    for (int i = 0; i < n; i++) {
      ParticleAttractor output = outputs[i];
      if (output.gotInput()) {
        if (i == n - 1 && outs[i].getText() != "") shift();
        outs[i].setText(output.input());
      }
    }
  }
  
  void shift() {
    for (int i = 0; i < n - 1; i++) {
      String next = outs[i+1].getText();
      outs[i].setText(next);
    }
  }
  
  void reset() {
    for (TextBox output : outs) {
      output.setText("");
    }
  }
  
  ArrayList<String> saveState() {
    ArrayList<String> state = new ArrayList<String>();
    state.add("#Output");
    state.addAll(super.saveState());
    for (TextBox output : outs) state.add(output.getText());
    return state;
  }
  
  void loadState(ArrayList<String> states) {
    int index = states.indexOf("#Output");
    ArrayList<String> state = new ArrayList<String>(states.subList(index + 1, index + 7));
    ArrayList<String> outputStates = new ArrayList<String>(states.subList(index + 7, index + 13));
    super.loadState(state);
    for (int i = 0; i < n; i++) outs[i].setText(outputStates.get(i));
    updatePos();
  }
  
  void helpOverlay() {
    show();
    fill(255);
    textSize(20);
    textAlign(CENTER, CENTER);
    text("Output from the Output Block will appear here. Up to 5 values will be stored at a time. -> ", x, y, -400, h);
  }
}