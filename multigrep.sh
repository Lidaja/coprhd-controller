FILES=$(grep -lr $1 | grep -v build)
#echo $FILES
for file in $FILES
do
echo $file
grep $2 $file
done
