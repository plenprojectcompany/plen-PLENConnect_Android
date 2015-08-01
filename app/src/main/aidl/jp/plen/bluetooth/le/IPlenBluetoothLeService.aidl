// IPlenBluetoothLeService.aidl
package jp.plen.bluetooth.le;

interface IPlenBluetoothLeService {
    void write(String data);
    void connectGatt(String deviceAddress);
    void disconnectGatt();
    int getState();
}