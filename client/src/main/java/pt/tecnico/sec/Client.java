package pt.tecnico.sec;

import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.util.*;

import java.security.*;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import pt.tecnico.sec.server.ServerFrontend;

public class Client {

    ServerFrontend _frontend;
    private int id;
    KeyStore  keyStore;

    private String pbkAlias;
    private String prkAlias;

    public Client( ServerFrontend frontend , int id){
        _frontend = frontend;
        this.id = id;
    }


    public boolean pingWorking( ){
        String input = "App";

        String s = _frontend.Ping( input );
        System.out.println( s );
        return true;
    }

    public void createKeyStore() {

        pbkAlias = "pbkAlias" + Integer.toString(id) ;
        prkAlias = "prkAlias" + Integer.toString(id) ;
        try {

            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA");
            //SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
            keyGen.initialize(1024 );
            KeyPair pair = keyGen.generateKeyPair();
            PrivateKey privateKey = pair.getPrivate();
            PublicKey publicKey = pair.getPublic();



            File file = new File("store" + Integer.toString(id) +".jks" );

            if( file.createNewFile() ){
                keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(null, "password".toCharArray() );
                FileOutputStream storeOut = new FileOutputStream("store" + Integer.toString(id) +".jks" );
                keyStore.store(storeOut, "password".toCharArray());
            }
            else{
                keyStore = KeyStore.getInstance("JKS");
                keyStore.load(new FileInputStream("store" + Integer.toString(id) +".jks"), "password".toCharArray());
            }

            if ( !keyStore.containsAlias(prkAlias) && !keyStore.containsAlias(pbkAlias) ) {
                //keystore.setEntry("alias", new KeyStore.PrivateKeyEntry(privateKey, null), new KeyStore.PasswordProtection("pass".toCharArray()));	//chain null, mas precisa do certificado
                X509Certificate cert = new Certificate() ;
                X509CertInfo info = new X509CertInfo();
                keyStore.setKeyEntry( prkAlias, privateKey, "password".toCharArray() , null);
                //keyStore.setKeyEntry( prkAlias , privateKey,"password".toCharArray(), null);//chain null(?)
            }

            /*
    Date from = new Date();
    Date to = new Date(from.getTime() + days * 86400000l);
    CertificateValidity interval = new CertificateValidity(from, to);
    BigInteger sn = new BigInteger(64, new SecureRandom());
    X500Name owner = new X500Name(dn);

    info.set(X509CertInfo.VALIDITY, interval);
    info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(sn));
    info.set(X509CertInfo.SUBJECT, new CertificateSubjectName(owner));
    info.set(X509CertInfo.ISSUER, new CertificateIssuerName(owner));
    info.set(X509CertInfo.KEY, new CertificateX509Key(pair.getPublic()));
    info.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
    AlgorithmId algo = new AlgorithmId(AlgorithmId.md5WithRSAEncryption_oid);
    info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algo));

    // Sign the cert to identify the algorithm that's used.
    X509CertImpl cert = new X509CertImpl(info);
    cert.sign(privkey, algorithm);

    // Update the algorith, and resign.
    algo = (AlgorithmId)cert.get(X509CertImpl.SIG_ALG);
    info.set(CertificateAlgorithmId.NAME + "." + CertificateAlgorithmId.ALGORITHM, algo);
    cert = new X509CertImpl(info);
    cert.sign(privkey, algorithm);*/
            saveKeyStore();

        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    void saveKeyStore(){
        FileOutputStream storeOut = null;
        try {
            storeOut = new FileOutputStream("store" + Integer.toString(id) +".jks" );
            keyStore.store(storeOut, "password".toCharArray());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}
