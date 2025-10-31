```
-Xmx4g -da -dsa -XX:+UnlockExperimentalVMOptions -XX:MaxGCPauseMillis=1 --add-modules jdk.incubator.vector

-XX:MaxGCPauseMillis=1 -XX:FreqInlineSize=1000 -XX:MaxInlineSize=1000 -XX:MaxRecursiveInlineLevel=15 -XX:InlineSmallCode=10000 -XX:MaxInlineLevel=25 -XX:-DontCompileHugeMethods -XX:OnStackReplacePercentage=300 -XX:EscapeAnalysisTimeout=100 -XX:+OptoScheduling -XX:+OptoBundling -XX:+OptimizeFill -XX:+UseFastStosb -XX:+UseVectorCmov

-XX:MaxGCPauseMillis=1
    //default=200ms
-XX:FreqInlineSize=1000
    # default=325
-XX:MaxInlineSize=1000
    # default=35
-XX:MaxRecursiveInlineLevel=15
    # default=1
-XX:InlineSmallCode=10000:
    # default=2500
-XX:MaxInlineLevel=25
    # default=15
-XX:-DontCompileHugeMethods
    #default = true
-XX:OnStackReplacePercentage=300
    #default = 140
-XX:EscapeAnalysisTimeout=100
    #default = 20
-XX:+OptoScheduling
-XX:+OptoBundling
-XX:+OptimizeFill
-XX:+UseVectorCmov
    #default = false
```

# Garbage Collection Tuning

## ZeroGC
for low latency, useful in real-time
```
-XX:+UnlockExperimentalVMOptions -XX:+UseZGC
-XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC
```

## G1
https://www.oracle.com/technical-resources/articles/java/g1gc.html

# Graal notes
```
java -XX:+AggressiveOpts -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+PrintFlagsFinal -version

-XX:+UseLargePages

-XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -XX:+UseJVMCICompiler

~/graalvm-ce-java16-21.1.0-dev/bin/native-image --verbose -dsa -da -jar ~/n/narchy/lab/target/n.jar
```
## Print VM Flags
```
java -XX:+UnlockExperimentalVMOptions -XX:+EagerJVMCI -Dgraal.ShowConfiguration=info -XX:+EnableJVMCI -XX:+JVMCIPrintProperties
```
```
-Dgraal.AlwaysInlineIntrinsics=true
-Dgraal.EscapeAnalysisIterations=10
-Dgraal.InliningDepthError=2000
-Dgraal.MaximumDesiredSize=80000
-Dgraal.MaximumInliningSize=1000
-Dgraal.MaximumRecursiveInlining=10
-Dgraal.SmallCompiledLowLevelGraphSize=1024
```
