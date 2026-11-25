# Retrofit and kotlinx.serialization ship consumer rules. Preserve generic API signatures
# and runtime annotations used to locate Retrofit suspend service methods.
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault
# Named companion objects need explicit serializer retention if introduced later.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> { static <1>$Companion Companion; }
