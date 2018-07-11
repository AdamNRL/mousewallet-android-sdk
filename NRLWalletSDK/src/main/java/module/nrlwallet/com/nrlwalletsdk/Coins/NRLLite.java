package module.nrlwallet.com.nrlwalletsdk.Coins;

import android.os.Build;
import android.support.annotation.RequiresApi;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.BitcoinSerializer;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionBroadcast;
import org.bitcoinj.core.TransactionBroadcaster;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDUtils;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.SPVBlockStore;
import org.bitcoinj.utils.MonetaryFormat;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.crypto.Credentials;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Date;
import java.util.Arrays;
import java.util.List;

import io.github.novacrypto.bip32.ExtendedPrivateKey;
import io.github.novacrypto.bip32.ExtendedPublicKey;
import io.github.novacrypto.bip32.Network;
import io.github.novacrypto.bip32.derivation.CkdFunctionDerive;
import io.github.novacrypto.bip32.derivation.Derive;
import io.github.novacrypto.bip32.networks.Bitcoin;
import io.github.novacrypto.bip32.networks.Litecoin;
import io.github.novacrypto.bip44.Account;
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
import module.nrlwallet.com.nrlwalletsdk.Utils.LitecoinNetParams;
import module.nrlwallet.com.nrlwalletsdk.Utils.WIF;
import module.nrlwallet.com.nrlwalletsdk.abstracts.NRLCallback;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class NRLLite extends NRLCoin {
    String url_server = "https://ltc.mousebelt.com/api/v1";
    Network network = Bitcoin.MAIN_NET;
    int coinType = 2;
    String seedKey = "Bitcoin seed";
    String curve = "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141";
    byte[] bSeed;
    String rootKey;
    AddressIndex addressIndex;
    String extendedPrivateKey;
    String extendedPublicKey;
    private String walletAddress;
    String privateKey;
    int count = 0;
    String Wif = "";
    String balance = "0";
    JSONArray transactions = new JSONArray();

    Wallet wallet;
    String str_seed;
    File chainFile;

    private List<Integer> expected;
    private String path;
    private int[] list;

    public NRLLite(byte[] seed, String s_seed) {

        super(seed, Litecoin.MAIN_NET, 2, "Bitcoin seed", "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141");
        bSeed = seed;
        str_seed = s_seed;
        createWallet();
//        this.getData(seed);
    }


    private void createWallet() {
        Long creationtime = new Date().getTime();
        NetworkParameters params = LitecoinNetParams.get();
        try {
            DeterministicSeed seed = new DeterministicSeed(str_seed, bSeed, "", creationtime);

            wallet = Wallet.fromSeed(params, seed);
            walletAddress = wallet.currentReceiveAddress().toBase58();
//            privateKey = wallet.getActiveKeyChain().getWatchingKey().getPrivKey().toString();
            wallet.clearTransactions(0);
            chainFile = new File(android.os.Environment.getExternalStorageDirectory(),"lite.spvchain");

            if (chainFile.exists()) {
                chainFile.delete();
            }

            balance = wallet.getBalance().toString();

            // Setting up the BlochChain, the BlocksStore and connecting to the network.
            SPVBlockStore chainStore = new SPVBlockStore(params, chainFile);
            BlockChain chain = new BlockChain(params, chainStore);
            PeerGroup peerGroup = new PeerGroup(params, chain);
            peerGroup.addPeerDiscovery(new DnsDiscovery(params));

            // Now we need to hook the wallet up to the blockchain and the peers. This registers event listeners that notify our wallet about new transactions.
            chain.addWallet(wallet);
            peerGroup.addWallet(wallet);
            DownloadProgressTracker bListener = new DownloadProgressTracker() {
                @Override
                public void doneDownload() {
                    System.out.println("blockchain downloaded");
                }
            };

            // Now we re-download the blockchain. This replays the chain into the wallet. Once this is completed our wallet should know of all its transactions and print the correct balance.
            peerGroup.start();
            peerGroup.startBlockChainDownload(bListener);

            // Print a debug message with the details about the wallet. The correct balance should now be displayed.
            System.out.println(wallet.toString());

            // shutting down again
            peerGroup.stop();
        } catch (UnreadableWalletException e) {
            e.printStackTrace();
        } catch (BlockStoreException e) {
            e.printStackTrace();
        }
    }

    private void init() {
        ExtendedPrivateKey root = ExtendedPrivateKey.fromSeed(bSeed, Litecoin.MAIN_NET);
        walletAddress = root
                .derive("m/44'/2'/0'/0/0")
                .neuter().p2pkhAddress();
        Account account = BIP44.m().purpose44()
                .coinType(2)
                .account(0);
        final ExtendedPublicKey accountKey = root.derive(account, Account.DERIVATION).neuter();

        final ExtendedPrivateKey privateKey = root.derive("m/44'/2'/0'");
        extendedPrivateKey = privateKey.extendedBase58();
        extendedPublicKey = accountKey.extendedBase58();

    }

    public String getPrivateKey() {
        return this.privateKey;
    }

    @Override
    public String getAddress() {
        return walletAddress;
    }

    private void getTransactionCount() {
        String url_getbalance = url_server + "/address/gettransactioncount/" + this.walletAddress;
        new HTTPRequest().run(url_getbalance, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String result =   (response.body().string());
                    try {
                        JSONObject jsonObj = new JSONObject(result);
                        String msg = jsonObj.get("msg").toString();
                        if(msg.equals("success")) {
                            count = jsonObj.getInt("data");
                        }else {
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    // Request not successful
                }
            }
        });
    }
    public void getBalance(NRLCallback callback) {
        this.checkBalance(callback);
    }

    public void getTransactions(NRLCallback callback) {
        String url_getTransaction = url_server + "/address/txs/" + this.walletAddress;
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
                            Double vValue = new Double(0);
                            JSONArray transactionArray = new JSONArray();
                            for(int i = 0; i < transactions.length(); i++) {
                                JSONObject detail = (JSONObject) transactions.get(i);
                                JSONArray vout = detail.getJSONArray("vout");
                                JSONArray vin = detail.getJSONArray("vin");
                                Double voutVal = new Double(0);;
                                Double vinVal = new Double(0);;
                                for(int j = 0; j < vout.length(); j++) {
                                    JSONObject voutDetail = ((JSONObject)vout.get(j)).getJSONObject("scriptPubKey");
                                    JSONArray addresses = voutDetail.getJSONArray("addresses");
                                    boolean isaddress = false;
                                    for(int k = 0; k < addresses.length(); k++) {
                                        if(addresses.getString(k).equals(walletAddress)) {
                                            isaddress = true;
                                            break;
                                        }
                                    }
                                    if(isaddress){
                                        voutVal += ((JSONObject)vout.get(j)).getDouble("value");
                                    }
                                }
                                for(int j = 0; j < vin.length(); j++) {
                                    JSONObject vinDetail = (JSONObject)vin.get(j);
                                    JSONObject address = vinDetail.getJSONObject("address");
                                    JSONArray addresses = address.getJSONObject("scriptPubKey").getJSONArray("addresses");
                                    boolean isaddress = false;
                                    for(int k = 0; k < addresses.length(); k++) {
                                        if(addresses.getString(k).equals(walletAddress)) {
                                            isaddress = true;
                                            break;
                                        }
                                    }
                                    if(isaddress){
                                        vinVal += address.getDouble("value");
                                    }
                                }
                                vValue = vinVal - voutVal;
                                JSONObject transactionData = new JSONObject();
                                transactionData.put("value", vValue);
                                transactionData.put("txid", detail.getString("txid"));
                                transactionArray.put(transactionData);
                            }
                            callback.onResponseArray(transactionArray);
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
        String url_getbalance = url_server + "/balance/" + this.walletAddress;
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

    public void getTransactionsJson(NRLCallback callback) {
        String url_getTransaction = url_server + "/address/txs/" + this.walletAddress;
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
                } else {
                }
            }
        });

    }

    public void createTransaction(String amount, String address, String memo, long fee, NRLCallback callback) {

        NetworkParameters params = LitecoinNetParams.get();
        Coin value = Coin.parseCoin(amount);
        try {
            Address to = new Address(params, address);
            TransactionBroadcaster transactionBroadcaster = new TransactionBroadcaster() {
                @Override
                public TransactionBroadcast broadcastTransaction(Transaction tx) {
//                    callback.onResponse(tx.toString());
                    callback.onResponse("success " + tx.toString());
                    return null;
                }
            };
            wallet.sendCoins(transactionBroadcaster, to, value);
        } catch (InsufficientMoneyException e) {
            callback.onFailure(e);
        }

    }

}
