#!/bin/bash
dirname=""
for var in "$@"
do
    echo "$var"

    if [ -z "$dirname" ]
    then
        dirname=`readlink -e "$var"`
        dirname=`dirname "$dirname"`
        echo `date` > "${dirname}/accuracy.txt"
    fi

    total=0
    exactHits=0
    by1Misses=0
    by2Misses=0

    confusionMatrixString=`sed -n '/=== Confusion Matrix ===/,$p' $var`
    prefix="=== Confusion Matrix === a b c d e f g h i j k l m n o p q r s t u v w x y z aa ab ac ad ae af <-- classified as "
    confusionMatrixString=`echo $confusionMatrixString | sed -e "s/^$prefix//"`
    array=(${confusionMatrixString// / })
    declare -A confusionMatrix
    declare -A places
    for i in {0..31}
    do
        for j in {0..31}
        do
            index=$(($i * 36 + $j))
            confusionMatrix[$i,$j]="${array[index]}"
            total=$(($total + ${array[index]}))
        done
    done
    for i in {0..31}
    do
        index=$(($i * 36 + 35))
        places[$i]="${array[index]}"
    done

    declare -A neighbors

    neighbors["121"]="237schody121 141"
    neighbors["125"]="126"
    neighbors["126"]="125 127"
    neighbors["127"]="126 128winda 223schody127"
    neighbors["128winda"]="128a 127"
    neighbors["128a"]="130 128winda"
    neighbors["130"]="132 128a"
    neighbors["132"]="133 130"
    neighbors["133"]="135 132"
    neighbors["135"]="136 133"
    neighbors["136"]="139 135"
    neighbors["139"]="141 136"
    neighbors["141"]="121 139"
    neighbors["221"]="222"
    neighbors["222"]="223 221"
    neighbors["223"]="223schody127 224winda 222"
    neighbors["224winda"]="224 223"
    neighbors["224"]="225 224winda"
    neighbors["225"]="226 224"
    neighbors["226"]="227 225"
    neighbors["227"]="229schody 226"
    neighbors["229schody"]="229schody133 230 227"
    neighbors["230"]="229schody 232"
    neighbors["232"]="233 230"
    neighbors["233"]="235 232"
    neighbors["235"]="236 233"
    neighbors["236"]="237schody 235"
    neighbors["237/238"]="237schody"
    neighbors["237schody"]="237schody121 237/238 236"
    neighbors["237schody121"]="237schody 121"
    neighbors["229schody133"]="229schody"
    neighbors["223schody127"]="127 223"

    midSections=("223schody127" "229schody133" "237schody121")

    containsElement () {
      local e match="$1"
      shift
      for e; do [[ "$e" == "$match" ]] && return 0; done
      return 1
    }

    function placeIndex {
        for l in {0..31}
        do
            if [ "$1" == "${places[$l]}" ]
            then
                return $l
            fi
        done
    }

    for i in {0..31}
    do
        exactHits=$((exactHits+confusionMatrix[$i,$i]))
        place=${places[$i]}
        placeNeighbors=${neighbors[$place]}
        for j in ${placeNeighbors// / }
        do
            placeIndex "$j"
            neighbourIndex=$?
            by1Misses=$((by1Misses + confusionMatrix[$i,$neighbourIndex]))
            neighborPlaceNeighbors=${neighbors[$j]}
            for k in ${neighborPlaceNeighbors// / }
            do
                if [ "${place}" != "${k}" ]
                then
                    containsElement "${k}" "${midSections[@]}"
                    if [ $? ]
                    then
                        placeIndex "$k"
                        neighbourIndex=$?
                        by2Misses=$((by2Misses + confusionMatrix[$i,$neighbourIndex]))
                    fi
                fi
            done
        done
    done

    echo $total
    echo $exactHits
    echo $by1Misses
    echo $by2Misses

    exactAccuracy=`bc -l <<< "($exactHits)*100/$total"`
    by1MissAccuracy=`bc -l <<< "($exactHits + $by1Misses)*100/$total"`
    by2MissAccuracy=`bc -l <<< "($exactHits + $by1Misses + $by2Misses)*100/$total"`
    echo $exactAccuracy
    echo $by1MissAccuracy
    echo $by2MissAccuracy

    echo "Dokładność $var" >> "${dirname}/accuracy.txt"
    echo "Do jednego miejsca: $exactAccuracy %" >> "${dirname}/accuracy.txt"
    echo "Do dwóch miejsc: $by1MissAccuracy %" >> "${dirname}/accuracy.txt"
    echo "Do trzech miejsc: $by2MissAccuracy %" >> "${dirname}/accuracy.txt"
    echo "" >> "${dirname}/accuracy.txt"
done