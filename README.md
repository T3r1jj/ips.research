# Indoor Positioning System research
This is a Indoor Positioning System research project that I have done to improve [PBMap](https://github.com/T3r1jj/PBMap) navigation. This project incorporates
an Android app for data collection and partial testing, bash scripts for running Weka tests, and custom k-NN distance metric in form of Weka package.
The aim was to test **accelerometer based pedometer** and **WiFi fingerprinting**.

There are complex frameworks that may provide very good accuracy, like [FIND](https://github.com/schollz/find3). However, they are often based
on the client-server architecture. The users might not have access to the building WiFi network and to the cellular data (no signal or just preference).
The point of this research was to test the accuracy and feasibility of **an offline/standalone system**.
Another important and shared factors were (among other things) **heterogenity of devices** and **changes in the environment** (like presence of people/APs).
More information with results and user's guide will be published after few months due to legal requirements.

### App main features
1. Data collection (with device and sensor information)
    - inertial (acceleration samples, movement type, sampling frequency)
    - magnetic (magnetic field, gravity, route name)
    - WiFi (fingerprints: SSID, BSSID, RSSI, timestamp, place)
2. Data storage
    - sdcard and remote (free CouchDB hosting can be used)
    - loading, saving and removal
3. Algorithm offline testing (semi-automated)
    - pedometer - on device (all possible combinations):
      - movement type
      - frequency
      - device
      - filtering choice: none, Kalman filtering, moving average filter
      - algorithm parameters: sensitivity (custom or automatic)
    - RSSI fingerprinting :
      - on device - k-NN quick pre-testing
      - on PC - supported by bash scripts that run Weka tests and calculate accuracy with three-level precision (neighbours)
      - SSID filtering (regex)
      - datasets permutations (device groups and dates)
      - different value scales: logharitmic and linear
      - datasets division and grouping (average/median/none)
3. Algorithm online testing (manual)
    - checking if performance is feasible on the device (standalone version)
    - checking if pedometer counts steps for random people

### Technology stack  
An [Anvil](https://github.com/zserge/anvil) library has been used together with Kotlin for very fast prototyping. Aside from that:
- MPAndroidChart
- Weka library and application
- local Couchbase with remote CouchDB
- JUnit
- Bash

### Gallery

![menu](https://user-images.githubusercontent.com/20327242/46581259-ca3ef080-ca35-11e8-9b42-7ec062769bcd.png) | ![inertial](https://user-images.githubusercontent.com/20327242/46581256-c9a65a00-ca35-11e8-851a-376f3e5eaa60.png)
------------ | -------------
![inertial2](https://user-images.githubusercontent.com/20327242/46581257-c9a65a00-ca35-11e8-82d5-02728def0a9a.png) | ![wifi](https://user-images.githubusercontent.com/20327242/46581252-c90dc380-ca35-11e8-807c-e46518311229.png)
![data_load2](https://user-images.githubusercontent.com/20327242/46581255-c9a65a00-ca35-11e8-8aea-4698c32d1abd.png) | ![arff_test](https://user-images.githubusercontent.com/20327242/46581253-c9a65a00-ca35-11e8-951f-0172f40c0966.png)
![arff2](https://user-images.githubusercontent.com/20327242/46581254-c9a65a00-ca35-11e8-93cf-6b397c434e31.png) | ![pedometer_test](https://user-images.githubusercontent.com/20327242/46581261-ca3ef080-ca35-11e8-8e70-70074db9fb86.png)
![pedometer_test_output](https://user-images.githubusercontent.com/20327242/46581251-c90dc380-ca35-11e8-8920-2b1da7ffe61e.png) | ![info](https://user-images.githubusercontent.com/20327242/46581258-ca3ef080-ca35-11e8-8575-128c68f4db1b.png)
![online](https://user-images.githubusercontent.com/20327242/46581260-ca3ef080-ca35-11e8-8039-be7263fd591b.png) | 
