package hardwar.branch.prediction.judged.PAp;


import hardwar.branch.prediction.shared.*;
import hardwar.branch.prediction.shared.devices.*;

import java.util.Arrays;

public class PAp implements BranchPredictor {

    private final int branchInstructionSize;

    private final ShiftRegister SC; // saturating counter register

    private final RegisterBank PABHR; // per address branch history register

    private final Cache<Bit[], Bit[]> PAPHT; // Per Address Predication History Table

    public PAp() {
        this(4, 2, 8);
    }

    public PAp(int BHRSize, int SCSize, int branchInstructionSize) {
        // TODO: complete the constructor
        this.branchInstructionSize = branchInstructionSize;


        // Initialize the BHR register with the given size and no default value
        this.PABHR = new RegisterBank(branchInstructionSize, BHRSize);

        // Initialize the PHT with a size of 2^size and each entry having a saturating counter of size "SCSize"
        PAPHT = new PageHistoryTable(BHRSize+branchInstructionSize, SCSize);

        // Initialize the SC register
        SC = new SIPORegister("SC", SCSize, null);
    }

    @Override
    public BranchResult predict(BranchInstruction branchInstruction) {
        // TODO: complete Task 1
        Bit[] newAddress= getCacheEntry(branchInstruction.getInstructionAddress(),PABHR.read(branchInstruction.getInstructionAddress()).read());

        PAPHT.putIfAbsent(newAddress, getDefaultBlock());
        SC.load(PAPHT.get(newAddress));
        return (BranchResult.of((SC.read()[0]).getValue()));

    }

    @Override
    public void update(BranchInstruction instruction, BranchResult actual) {
        // TODO:complete Task 2
        Bit[] newAddress= getCacheEntry(instruction.getInstructionAddress(),PABHR.read(instruction.getInstructionAddress()).read());

        Bit[] updated_value=CombinationalLogic.count(SC.read(), actual==BranchResult.TAKEN, CountMode.SATURATING);
        PAPHT.put(newAddress, updated_value);
        ShiftRegister a=PABHR.read(instruction.getInstructionAddress());
        a.insert(Bit.of(actual==BranchResult.TAKEN));
        PABHR.write(instruction.getInstructionAddress(), a.read());
    }


    private Bit[] getCacheEntry(Bit[] branchAddress, Bit[] BHRValue) {
        // Concatenate the branch address bits with the BHR bits
        Bit[] cacheEntry = new Bit[branchAddress.length + BHRValue.length];
        System.arraycopy(branchAddress, 0, cacheEntry, 0, branchInstructionSize);
        System.arraycopy(BHRValue, 0, cacheEntry, branchAddress.length, BHRValue.length);
        return cacheEntry;
    }

    /**
     * @return a zero series of bits as default value of cache block
     */
    private Bit[] getDefaultBlock() {
        Bit[] defaultBlock = new Bit[SC.getLength()];
        Arrays.fill(defaultBlock, Bit.ZERO);
        return defaultBlock;
    }

    @Override
    public String monitor() {
        return "PAp predictor snapshot: \n" + PABHR.monitor() + SC.monitor() + PAPHT.monitor();
    }
}
