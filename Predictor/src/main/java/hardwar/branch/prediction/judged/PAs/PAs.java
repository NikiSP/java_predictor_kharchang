package hardwar.branch.prediction.judged.PAs;


import hardwar.branch.prediction.shared.*;
import hardwar.branch.prediction.shared.devices.*;

import java.util.Arrays;

public class PAs implements BranchPredictor {

    private final int branchInstructionSize;
    private final int KSize;
    private final HashMode hashMode;
    private final ShiftRegister SC; // saturating counter register
    private final RegisterBank PABHR; // per address Branch History Register
    private final Cache<Bit[], Bit[]> PSPHT; // Per Set Predication History Table

    public PAs() {
        this(4, 2, 8, 4, HashMode.XOR);
    }

    public PAs(int BHRSize, int SCSize, int branchInstructionSize, int KSize, HashMode hashMode) {
        // TODO: complete the constructor
        this.branchInstructionSize = 0;
        this.KSize = KSize;
        this.hashMode = HashMode.XOR;



        // Initialize the BHR register with the given size and no default value
        this.PABHR = new RegisterBank(KSize, BHRSize);

        // Initialize the PHT with a size of 2^size and each entry having a saturating counter of size "SCSize"
        PSPHT = new PageHistoryTable(BHRSize+KSize, SCSize);

        // Initialize the SC register
        SC = new SIPORegister("SC", SCSize, null);
    }

    /**
     * predicts the result of a branch instruction based on the per address BHR and hash value of branch
     * instruction address
     *
     * @param branchInstruction the branch instruction
     * @return the predicted outcome of the branch instruction (taken or not taken)
     */
    @Override
    public BranchResult predict(BranchInstruction branchInstruction) {
        // TODO: complete Task 1
        Bit[] newAddress= getCacheEntry(branchInstruction.getInstructionAddress(),PABHR.read(branchInstruction.getInstructionAddress()).read());

        PSPHT.putIfAbsent(newAddress, getDefaultBlock());
        SC.load(PSPHT.get(newAddress));
        return (BranchResult.of((SC.read()[0]).getValue()));
    }

    @Override
    public void update(BranchInstruction instruction, BranchResult actual) {
        // TODO:complete Task 2
        Bit[] newAddress= getCacheEntry(instruction.getInstructionAddress(),PABHR.read(instruction.getInstructionAddress()).read());

        Bit[] updated_value=CombinationalLogic.count(SC.read(), actual==BranchResult.TAKEN, CountMode.SATURATING);
        PSPHT.put(newAddress, updated_value);
        ShiftRegister a=PABHR.read(instruction.getInstructionAddress());
        a.insert(Bit.of(actual==BranchResult.TAKEN));
        PABHR.write(instruction.getInstructionAddress(), a.read());
    }

    @Override
    public String monitor() {
        return "PAs predictor snapshot: \n" + PABHR.monitor() + SC.monitor() + PSPHT.monitor();
    }

    private Bit[] getCacheEntry(Bit[] branchAddress, Bit[] BHRValue) {
        // hash the branch address
        Bit[] hashKSize = CombinationalLogic.hash(branchAddress, KSize, hashMode);

        // Concatenate the Hash bits with the BHR bits
        Bit[] cacheEntry = new Bit[hashKSize.length + BHRValue.length];
        System.arraycopy(hashKSize, 0, cacheEntry, 0, hashKSize.length);
        System.arraycopy(BHRValue, 0, cacheEntry, hashKSize.length, BHRValue.length);

        return cacheEntry;
    }


    private Bit[] getDefaultBlock() {
        Bit[] defaultBlock = new Bit[SC.getLength()];
        Arrays.fill(defaultBlock, Bit.ZERO);
        return defaultBlock;
    }
}
