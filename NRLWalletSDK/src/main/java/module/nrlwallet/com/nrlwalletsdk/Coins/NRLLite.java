package module.nrlwallet.com.nrlwalletsdk.Coins;

import android.os.Build;
import android.support.annotation.RequiresApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import io.github.novacrypto.bip32.ExtendedPrivateKey;
import io.github.novacrypto.bip32.ExtendedPublicKey;
import io.github.novacrypto.bip32.Network;
import io.github.novacrypto.bip32.derivation.CkdFunctionDerive;
import io.github.novacrypto.bip32.derivation.Derive;
import io.github.novacrypto.bip32.networks.Bitcoin;
import io.github.novacrypto.bip44.AddressIndex;
import io.github.novacrypto.bip44.BIP44;
import module.nrlwallet.com.nrlwalletsdk.Common.ValidationException;
import module.nrlwallet.com.nrlwalletsdk.Cryptography.Base58Encode;
import module.nrlwallet.com.nrlwalletsdk.Cryptography.Secp256k1;
import module.nrlwallet.com.nrlwalletsdk.Network.CoinType;
import module.nrlwallet.com.nrlwalletsdk.Utils.ExtendedKey;
import module.nrlwallet.com.nrlwalletsdk.Utils.ExtendedPrivateKeyBIP32;
import module.nrlwallet.com.nrlwalletsdk.Utils.HTTPRequest;
import module.nrlwallet.com.nrlwalletsdk.Utils.HexStringConverter;
import module.nrlwallet.com.nrlwalletsdk.Utils.WIF;
import module.nrlwallet.com.nrlwalletsdk.abstracts.NRLCallback;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class NRLLite extends NRLCoin {

    Network network = Bitcoin.MAIN_NET;
    int coinType = 2;
    String seedKey = "Bitcoin seed";
    String curve = "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141";
    byte[] bSeed;
    String rootKey;
    AddressIndex addressIndex;
    String extendedPrivateKey;
    String extendedPublicKey;
    String walletAddress;
    String privateKey;
    String balance = "0";
    JSONArray transactions = new JSONArray();


    private List<Integer> expected;
    private String path;
    private int[] list;

    public NRLLite(byte[] seed) {

        super(seed, Bitcoin.MAIN_NET, 2, "Bitcoin seed", "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141");
        bSeed = seed;
        this.getData(seed);
        this.init();
    }

    private int[] copy(List<Integer> expected) {
        final int length = expected.size();
        final int[] list = new int[length];
        for (int i = 0; i < length; i++)
            list[i] = expected.get(i);
        return list;
    }

    private void getData(byte[] bSeed) {
        try {
            ExtendedKey m = ExtendedKey.create(bSeed);
            // Derive account based on BIP44
            ExtendedKey ka = module.nrlwallet.com.nrlwalletsdk.Utils.BIP44.getKeyType(m, coinType, 0, 0);

            // Print first 5 addresses of the first account.
            for (int i = 0; i < 1; i++) {
                ExtendedKey ck = ka.getChild(i);
                String addr = ck.getAddress();
                String wif = WIF.getWif(ck.getMaster());
                System.out.println("acct 0 ,idx:" + i + " ,addr:" + addr + " ,wif:" + wif);
            }
        } catch (ValidationException e) {
            e.printStackTrace();
        }

    }

    private void init() {
        ExtendedPrivateKey root = ExtendedPrivateKey.fromSeed(bSeed, network);
        addressIndex = BIP44.m()
                .purpose44()
                .coinType(coinType)
                .account(0)
                .external()
                .address(0);

        this.walletAddress = root.derive(addressIndex, AddressIndex.DERIVATION)
                .neuter().p2pkhAddress();



        Derive<Integer[]> derive = new CkdFunctionDerive<>(NRLLite::concat, new Integer[0]);
        Integer[] actual = derive.derive(addressIndex, AddressIndex.DERIVATION);

        this.rootKey = new ExtendedPrivateKeyBIP32().getRootKey(bSeed, CoinType.ETHEREUM);
        ExtendedPrivateKey privateKey;
        privateKey = ExtendedPrivateKey.fromSeed(bSeed, Bitcoin.MAIN_NET);
        ExtendedPrivateKey child = privateKey.derive("m/44'/2'/0'/0");
        ExtendedPublicKey childPub = child.neuter();
        extendedPrivateKey = child.extendedBase58();   //Extended Private Key
        extendedPublicKey = childPub.extendedBase58();    //Extended Public Key
        walletAddress = childPub.p2pkhAddress();
        String str4 = childPub.p2shAddress();

    }

    public String getPrivateKey() {
        return this.privateKey;
    }

    @Override
    public String getAddress() {
        return walletAddress;
    }

    public void getBalance(NRLCallback callback) {
        this.checkBalance(callback);
    }

    public void getTransactions(NRLCallback callback) {
        this.checkTransactions(callback);
    }

    private void generatePubkeyFromPrivatekey(byte[] seed) {
        byte[] publickey = Secp256k1.getPublicKey(seed);
        String bbb = HexStringConverter.getHexStringConverterInstance().asHex(publickey);
        String aaa = Base58Encode.encode(publickey);
        System.out.println("************----------- Bitcoin public key     : " + aaa);
    }

    @RequiresApi(api = Build.VERSION_CODES.GINGERBREAD)
    private static Integer[] concat(Integer[] input, int index) {
        final Integer[] integers = Arrays.copyOf(input, input.length + 1);
        integers[input.length] = index;
        return integers;
    }
    public void checkBalance(NRLCallback callback) {
        String url_getbalance = "/balance/" + this.walletAddress;
        new HTTPRequest().run(url_getbalance, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String result =   (response.body().string());

                    try {
                        JSONObject jsonObj = new JSONObject(result);
                        String msg = jsonObj.get("msg").toString();
                        if(msg.equals("success")) {
                            JSONObject data = jsonObj.getJSONObject("data");
                            balance = data.getString("balance");
                            callback.onResponse(balance);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    // Do what you want to do with the response.
                } else {
                    // Request not successful
                }
            }
        });
    }

    private void checkTransactions(NRLCallback callback) {
        String url_getTransaction = "/address/txs/" + this.walletAddress;
        new HTTPRequest().run(url_getTransaction, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String body =   (response.body().string());

                    try {
                        JSONObject jsonObj = new JSONObject(body);
                        String msg = jsonObj.get("msg").toString();
                        if(msg.equals("success")) {
                            JSONObject data = jsonObj.getJSONObject("data");
                            transactions = data.getJSONArray("result");
                            callback.onResponseArray(transactions);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    // Do what you want to do with the response.
                } else {
                    // Request not successful
                }
            }
        });
    }

    public void createTransaction(long amount, String address, String memo, long fee) {

    }

}
