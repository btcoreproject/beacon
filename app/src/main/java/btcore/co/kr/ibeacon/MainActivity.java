package btcore.co.kr.ibeacon;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.View;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.distance.AndroidModel;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;


public class MainActivity extends BaseActivity implements BeaconConsumer {

    /**
     * BEACON_PARSER 에 들어 갈 수 있는 문자열 목록
     */
    private static final String BEACON_PARSER = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25";
    public static final String ALTBEACON_LAYOUT = "m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25";
    public static final String EDDYSTONE_TLM_LAYOUT = "x,s:0-1=feaa,m:2-2=20,d:3-3,d:4-5,d:6-7,d:8-11,d:12-15";
    public static final String EDDYSTONE_UID_LAYOUT = "s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19";
    public static final String EDDYSTONE_URL_LAYOUT = "s:0-1=feaa,m:2-2=10,p:3-3:-41,i:4-21v";
    public static final String URI_BEACON_LAYOUT = "s:0-1=fed8,m:2-2=00,p:3-3:-41,i:4-21v";

    private DecimalFormat decimalFormat = new DecimalFormat("#.##");

    private static final String TAG = "MainActivity";

    /**
     * 0이 아닌 숫자이면 된다.
     */
    private static final int REQUEST_ENABLE_BT = 100;

    /**
     * 플로팅 버튼 선언
     */
    private FloatingActionButton scanBleFAB;

    /**
     * 블루투스 어댑터 설정
     */
    private BluetoothAdapter mBluetoothAdapter;
    /**
     * 비콘 매니저 선언
     */
    private BeaconManager mBeaconManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**
         * Model 은 Data 를 뜻한다 단순 데이터가 아닌 데이터를 관리/ 수집/ 수정 하는 부분.
         */
        AndroidModel am = AndroidModel.forThisDevice();
        Log.d("getManufacturer()",am.getManufacturer());

        /**
         * 블루투스 어댑터 가져오기
         * getdefaultAdapter() 메서드를 호출하여 블루투스 어댑터 가져오기
         */
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        /**
         * 블루투스 활성화가 안되어 있으면 블루투스 활성화 다이얼로그를 요청하고
         * 블루투스 활성화가 되어 있다면 비콘 매니저에 비콘 특성을 등록한다.
         */
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            // 비콘 매니저 인스턴스 획득
            mBeaconManager = BeaconManager.getInstanceForApplication(this);
            mBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BEACON_PARSER));
            //BeaconManager.setRssiFilterImplClass(ArmaRssiFilter.class);
        }

        // FloatingButton 초기화
        scanBleFAB = findViewById(R.id.scanBleFAB);

        /**
         * 버튼 클릭 리스너
         */
        scanBleFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBeaconManager.isBound(MainActivity.this)) {
                    scanBleFAB.setImageResource(R.drawable.ic_visibility_white_24dp);
                    Log.i(TAG, "Stop BLE Scanning...");
                    // 비콘 매니저를 해제한다.
                    mBeaconManager.unbind(MainActivity.this);
                } else {
                    scanBleFAB.setImageResource(R.drawable.ic_visibility_off_white_24dp);
                    Log.i(TAG, "Start BLE Scanning...");
                    // 비콘 매니저를 등록한다. BeaconPaser 는 문자열이며 Ibeacon 과 EddyStone 을 구분한다.
                    mBeaconManager.bind(MainActivity.this);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 액티비티 종료시 비콘매니저 언바인드
        mBeaconManager.unbind(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            mBeaconManager = BeaconManager.getInstanceForApplication(this);
            mBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BEACON_PARSER));
            //BeaconManager.setRssiFilterImplClass(ArmaRssiFilter.class);
        }

    }

    /**
     * 비콘 서비스에 연결되면 onBeaconServiceConnect 메소드가 호출되고 이 메소드에서 Notifier(MonitorNotifier 또는 RangeNotifier)를 설정하고
     * Monitoring을 시작한다.
     */
    @Override
    public void onBeaconServiceConnect() {

        mBeaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            // 보이는 신호에 대한 mDistance 추정치를 제공하기 위해 초당 1회 호출.
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                // 비콘 사이즈가 0 이상이면 비콘이 식별되면.
                if (beacons.size() > 0) {
                    /**
                     * Iterator 는 자바의 컬렉션 프레임웍에서 컬렉션에 저장되어 있는 요소들을 읽어오는 방법을 표중화 하였는데 그중 하나가 Iterator 이다.
                     * 비콘 컬렉션에 저장되어 있는 요소들을 읽어 온다.
                     */
                    Iterator<Beacon> iterator = beacons.iterator();
                    // Iterator.hasNext() 메소드는 읽어 올 요소가 남아 있는지 확인하는 메소드.
                    while (iterator.hasNext()) {
                        // 읽을게 있으면.
                        Beacon beacon = iterator.next();
                        // beacon 에 저장 gn 비콘에 저장된 address, rssi, txpower, major, minor 값을 가져온다.
                        String address = beacon.getBluetoothAddress();
                            double rssi = beacon.getRssi();
                            int txPower = beacon.getTxPower();
                            double distance = Double.parseDouble(decimalFormat.format(beacon.getDistance()));
                            int major = beacon.getId2().toInt();
                            int minor = beacon.getId3().toInt();
                            Log.d(TAG, address+rssi+ txPower+ distance+ major+ minor);
                    }
                    /*
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            beaconAdapter = new BeaconAdapter(items, MainActivity.this);
                            binding.beaconListView.setAdapter(beaconAdapter);
                            beaconAdapter.notifyDataSetChanged();
                        }
                    });
                    */
                }
            }
        });
        try {
            // 비콘을 찾고 시작하는 Region 개체를 지역에서 비콘을 볼 수 있는동안 추정 Distance 에 있는 모든 초 업데이트를 제공.
            mBeaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        mBeaconManager.addMonitorNotifier(new MonitorNotifier() {

            @Override
            public void didEnterRegion(Region region) {
                Log.i(TAG, "I just saw an beacon for the first time!");
            }

            @Override
            public void didExitRegion(Region region) {
                Log.i(TAG, "I no longer see an beacon");
            }


            @Override
            public void didDetermineStateForRegion(int state, Region region) {
                    Log.i(TAG, "I have just switched from seeing/not seeing beacons: "+state);
            }
        });
        try {
            mBeaconManager.startMonitoringBeaconsInRegion(new Region("myMonitoringUniqueId", null, null, null));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
