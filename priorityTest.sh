

wcd /home/userfs/z/zs673/PriorityAssignment/result
pwd

rm -rf *.txt
cd /home/userfs/z/zs673/PriorityAssignment
pwd
rm nohup.out

javac $(find ./src/* | grep .java)

LD_LIBRARY_PATH=src nohup java -cp /home/userfs/z/zs673/PriorityAssignment/bin evaluation.CompleteExperiments &
