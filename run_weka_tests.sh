#!/bin/bash
#java -cp weka.jar  weka.core.WekaPackageManager -install-package WiFiDistance1.0.0.zip
echo "### Skrypt wymaga obecności weka.jar (z zainstalowaną paczką WiFiDistance1.0.0.zip) oraz parse_weka_result_buffers.sh (z uprawnieniami do wywołania)"
if [ $# == 0 ]
then
    echo "# Jako pierwszy parametr podaj ścieżkę do katalogu z grupą plików .arff (plik treningowy i testowe)"
    exit
fi
dirname=${1}

javaWithWekaCp="java -cp ./weka.jar"
total=20
current=0

declare -a functions=('weka.classifiers.bayes.NaiveBayes' '' '' 'weka.classifiers.trees.RandomForest -P 100 -I 100 -num-slots 1 -K 0 -M 1.0 -V 0.001 -S 1' '')
declare -a functionNames=('NaiveBayes' 'IBk.Euclidean' 'IBk.Manhattan' 'RandomForest' 'IBk.Euclidean.Noise')

arff=".*.arff"
ips="ips.wifi."
log=".log"
crossvalidation="-x 10"

mkdir "${dirname}out/"

trainDevice="train.Android-Combo"
declare -a testDevices=("test.Android-Combo" "test.Samsung-GT-N8000" "test.Unknown-GOCLEVER-TAB-A103")

function generateDeviceTests() {
    for (( i=0; i<${#functions[@]}; i++ ));
    do
        if [[ $1 == "test" ]]; then
            if [[ $i == 1 ]]; then
                ${javaWithWekaCp} weka.classifiers.lazy.IBk -K 1 -W 0 -A "weka.core.neighboursearch.LinearNNSearch -A \"weka.core.EuclideanDistance -R first-last\"" -o -t "${dirname}"${ips}${trainDevice}${arff} -T "${dirname}"${ips}${dev}${arff}  > "${dirname}out/"${dev}.${functionNames[i]}${log}
            elif [[ $i == 2 ]]; then
                ${javaWithWekaCp} weka.classifiers.lazy.IBk -K 1 -W 0 -A "weka.core.neighboursearch.LinearNNSearch -A \"weka.core.ManhattanDistance -R first-last\"" -o -t "${dirname}"${ips}${trainDevice}${arff} -T "${dirname}"${ips}${dev}${arff}  > "${dirname}out/"${dev}.${functionNames[i]}${log}
            elif [[ $i == 4 ]]; then
                if `grep -Fq "WIFI_dBm" "${dirname}"${ips}${dev}${arff}`
                then
                    ${javaWithWekaCp} weka.Run weka.classifiers.lazy.IBk -K 1 -W 0 -A "weka.core.neighboursearch.LinearNNSearch -A \"weka.core.DBmWiFiDistance -R first-last\"" -o -t "${dirname}"${ips}${trainDevice}${arff} -T "${dirname}"${ips}${dev}${arff}  > "${dirname}out/"${dev}.${functionNames[i]}${log}
                elif `grep -Fq "WIFI_pWatt" "${dirname}"${ips}${dev}${arff}`
                then
                    ${javaWithWekaCp} weka.Run weka.classifiers.lazy.IBk -K 1 -W 0 -A "weka.core.neighboursearch.LinearNNSearch -A \"weka.core.PWattWiFiDistance -R first-last\"" -o -t "${dirname}"${ips}${trainDevice}${arff} -T "${dirname}"${ips}${dev}${arff}  > "${dirname}out/"${dev}.${functionNames[i]}${log}
                else
                    echo "############ Corruppted file!!! ${dirname}"${ips}${dev}${arff}
                fi
            else
                ${javaWithWekaCp} ${functions[i]} -o -t "${dirname}"${ips}${trainDevice}${arff} -T "${dirname}"${ips}${dev}${arff} > "${dirname}out/"${dev}.${functionNames[i]}${log}
            fi
        else
            if [[ $i == 1 ]]; then
                ${javaWithWekaCp} weka.classifiers.lazy.IBk -K 1 -W 0 -A "weka.core.neighboursearch.LinearNNSearch -A \"weka.core.EuclideanDistance -R first-last\"" ${crossvalidation} -o -t "${dirname}"${ips}${dev}${arff} > "${dirname}out/"${dev}.${functionNames[i]}${log}
            elif [[ $i == 2 ]]; then
                ${javaWithWekaCp} weka.classifiers.lazy.IBk -K 1 -W 0 -A "weka.core.neighboursearch.LinearNNSearch -A \"weka.core.ManhattanDistance -R first-last\"" ${crossvalidation} -o -t "${dirname}"${ips}${dev}${arff} > "${dirname}out/"${dev}.${functionNames[i]}${log}
            elif [[ $i == 4 ]]; then
                if `grep -Fq "WIFI_dBm" "${dirname}"${ips}${dev}${arff}`
                then
                    ${javaWithWekaCp} weka.Run weka.classifiers.lazy.IBk -K 1 -W 0 -A "weka.core.neighboursearch.LinearNNSearch -A \"weka.core.DBmWiFiDistance -R first-last\"" ${crossvalidation} -o -t "${dirname}"${ips}${dev}${arff} > "${dirname}out/"${dev}.${functionNames[i]}${log}
                elif `grep -Fq "WIFI_pWatt" "${dirname}"${ips}${dev}${arff}`
                then
                    ${javaWithWekaCp} weka.Run weka.classifiers.lazy.IBk -K 1 -W 0 -A "weka.core.neighboursearch.LinearNNSearch -A \"weka.core.PWattWiFiDistance -R first-last\"" ${crossvalidation} -o -t "${dirname}"${ips}${dev}${arff} > "${dirname}out/"${dev}.${functionNames[i]}${log}
                else
                    echo "############ Corruppted file!!! ${dirname}"${ips}${dev}${arff}
                fi
            else
                ${javaWithWekaCp} ${functions[i]} ${crossvalidation} -o -t "${dirname}"${ips}${dev}${arff} > "${dirname}out/"${dev}.${functionNames[i]}${log}
            fi
        fi
        current=$((current + 1))
        echo "${current} / ${total}"
    done
}

dev=${trainDevice}

if `grep -Fq "WIFI_dBm" "${dirname}"${ips}${dev}${arff}`
then
    echo "dBm"
elif `grep -Fq "WIFI_pWatt" "${dirname}"${ips}${dev}${arff}`
then
    echo "pWatt"
else
    echo "############ Corruppted file!!! ${dirname}"${ips}${dev}${arff}
fi


generateDeviceTests "train crossvalidation"
for i in "${testDevices[@]}"
do
    dev=${i}
    crossvalidation="-T "${dirname}${ips}${dev}${arff}
    generateDeviceTests "test"
done

./parse_weka_result_buffers.sh "${dirname}"out/*.log