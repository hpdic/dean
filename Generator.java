import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Generator {
    public static String generateHash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            String encoded = Base64.getEncoder().encodeToString(hash);
            return encoded;
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Generator.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
   /* private String proofOfWork(Block block) {
    	String nonceKey = block.getNonce();
    	long nonce = 0;
    	boolean nonceFound = false;
    	String nonceHash = "";
    	Gson parser = new Gson();
    	String serializedData = parser.toJson(transactionPool);
    	String message = block.getTimeStamp() + block.getIndex() + block.getMerkleRoot() + serializedData
    	+ block.getPreviousHash();
    	while (!nonceFound) {
    	nonceHash = SHA256.generateHash(message + nonce);
    	nonceFound = nonceHash.substring(0, nonceKey.length()).equals(nonceKey);
    	nonce++;
    	}
    	return nonceHash;
    	}*/
}