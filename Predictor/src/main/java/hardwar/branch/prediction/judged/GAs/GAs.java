package hardwar.branch.prediction.judged.GAs;


import hardwar.branch.prediction.shared.*;
import hardwar.branch.prediction.shared.devices.*;

import java.util.Arrays;

public class GAs implements BranchPredictor {

    private final int branchInstructionSize;
    private final int KSize;
    private final HashMode hashMode;
    private final ShiftRegister SC; // saturating counter register
    private final ShiftRegister BHR; // branch history register
    private final Cache<Bit[], Bit[]> PSPHT; // Per Set Predication History Table

    public GAs() {
        this(4, 2, 8, 4, HashMode.XOR);
    }

    /**
     * Creates a new GAs predictor with the given BHR register size and initializes the PAPHT based on
     * the Ksize and saturating counter size
     *
     * @param BHRSize               the size of the BHR register
     * @param SCSize                the size of the register which hold the saturating counter value
     * @param branchInstructionSize the number of bits which is used for saving a branch instruction
     */
    public GAs(int BHRSize, int SCSize, int branchInstructionSize, int KSize, HashMode hashmode) {
        // TODO: complete the constructor
        this.KSize = KSize;
        this.hashMode = HashMode.XOR;
        // TODO: complete the constructor
        this.branchInstructionSize = branchInstructionSize;
        // Initialize the BHR register with the given size and no default value
        this.BHR = new SIPORegister("BHR", BHRSize, null);

        // Initialize the PHT with a size of 2^size and each entry having a saturating counter of size "SCSize"
        PSPHT = new PageHistoryTable((int)Math.pow(2,BHRSize+KSize), SCSize);

        // Initialize the SC register
        SC = new SIPORegister("SC", SCSize, null);
    }

    /**
     * predicts the result of a branch instruction based on the global branch history and hash value of
     * branch instruction address
     *
     * @param branchInstruction the branch instruction
     * @return the predicted outcome of the branch instruction (taken or not taken)
     */
    @Override
    public BranchResult predict(BranchInstruction branchInstruction) {
         // TODO: complete Task 1
         Bit[] newAddress= getCacheEntry(branchInstruction.getInstructionAddress());

         PSPHT.putIfAbsent(newAddress, getDefaultBlock());
         SC.load(PSPHT.get(newAddress));
         return (BranchResult.of((SC.read()[0]).getValue()));
    }

    /**
     * Updates the value in the cache based on actual branch result
     *
     * @param branchInstruction the branch instruction
     * @param actual            the actual result of branch (Taken or Not)
     */
    @Override
    public void update(BranchInstruction branchInstruction, BranchResult actual) {
        // TODO: complete Task 2
        Bit[] newAddress= getCacheEntry(branchInstruction.getInstructionAddress());

        Bit[] updated_value=CombinationalLogic.count(SC.read(), actual==BranchResult.TAKEN, CountMode.SATURATING);
        PSPHT.put(newAddress, updated_value);
        BHR.insert(Bit.of(actual==BranchResult.TAKEN));
    }

    /**
     * @return snapshot of caches and registers content
     */
    public String monitor() {
        return "GAp predictor snapshot: \n" + BHR.monitor() + SC.monitor() + PSPHT.monitor();
    }


    /**
     * concat the PC and BHR to retrieve the desired address
     *
     * @param branchAddress program counter
     * @return concatenated value of first M bits of branch address and BHR
     */
    private Bit[] getCacheEntry(Bit[] branchAddress) {
        // hash the branch address
        Bit[] hashKSize = CombinationalLogic.hash(branchAddress, KSize, hashMode);

        // Concatenate the Hash bits with the BHR bits
        Bit[] bhrBits = BHR.read();
        Bit[] cacheEntry = new Bit[hashKSize.length + bhrBits.length];
        System.arraycopy(hashKSize, 0, cacheEntry, 0, hashKSize.length);
        System.arraycopy(bhrBits, 0, cacheEntry, hashKSize.length, bhrBits.length);

        return cacheEntry;
    }

    private Bit[] getDefaultBlock() {
        Bit[] defaultBlock = new Bit[SC.getLength()];
        Arrays.fill(defaultBlock, Bit.ZERO);
        return defaultBlock;
    }
}
