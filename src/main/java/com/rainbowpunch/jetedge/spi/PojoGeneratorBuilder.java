package com.rainbowpunch.jetedge.spi;

import com.rainbowpunch.jetedge.core.FieldDataGenerator;
import com.rainbowpunch.jetedge.core.FieldSetter;
import com.rainbowpunch.jetedge.core.PojoAttributes;
import com.rainbowpunch.jetedge.core.analyzer.Analyzers;
import com.rainbowpunch.jetedge.core.analyzer.PojoAnalyzer;
import com.rainbowpunch.jetedge.core.limiters.Limiter;
import com.rainbowpunch.jetedge.core.reflection.ClassAttributes;
import com.rainbowpunch.jetedge.core.reflection.ConstructorParameter;
import com.rainbowpunch.jetedge.core.reflection.FieldAttributes;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Provides a fluent API to describe how a POJO of type <code>T</code> should be generated.
 *
 * All data generation stems from the simple syntax
 *
 * <br>
 * <br>
 * <code>
 *      PojoGenerator&lt;YourClass&gt; generator = new PojoGeneratorBuilder&lt;&gt;(YourClass.class)
 *      <br>
 *      ... limitation on data generation ...
 *      <br>
 *      .build();
 * </code>
 * <br>
 * <br>
 *
 * Each of the methods in this class provides a way to edit and alter the range of acceptable values that Jetedge should use when creating your
 * objects.
 * <br>
 * The majority of the features available here require the use of field names in order to specify how data creation/manipulation should be handled.
 * For nested objects, use a dot-delimited string to reference the field you are wanting to control. e.g. topPojo.innerObject.someValue
 * <br>
 * Unless otherwise specified, field and type discovery for POJOs is done by looking for set/get method (public or private) for all fields in a
 * class.  e.g. setName(String aString) and getName() for the field called name.  This behavior can be easily changed,
 * but it is the default if not otherwise specified.
 *
 * @param <T>
 *          The class that will should be generated.
 */
public final class PojoGeneratorBuilder<T> implements Cloneable {

    private static DefaultDataLimiter baseDataLimiters;

    private final Class<T> clazz;
    private final PojoAttributes<T> pojoAttributes;

    private PojoGeneratorBuilder(Class<T> clazz, PojoAttributes<T> pojoAttributes) {
        this.clazz = clazz;
        this.pojoAttributes = pojoAttributes;
        if (baseDataLimiters != null) {
            this.setDefaultDataGenerators(baseDataLimiters);
        }
    }

    /**
     * Creates a builder object for the PojoGenerator with the defined class
     * @param clazz
     *          The class of objects that the PojoGenerator should create.
     */
    public PojoGeneratorBuilder(Class<T> clazz) {
        this(clazz, new Random().nextInt());
    }

    /**
     * Creates a builder object for the PojoGenerator with the defined class
     * @param clazz
     *          The class of objects that the PojoGenerator should create.
     * @param pojoAnalyzer
     *          A predefined way to analyze your POJOs structure and determine how to build.
     */
    public PojoGeneratorBuilder(Class<T> clazz, PojoAnalyzer pojoAnalyzer) {
        this(clazz, new Random().nextInt(), pojoAnalyzer);
    }

    /**
     * Creates a builder object for the PojoGenerator with the defined class
     * @param clazz
     *          The class of objects that the PojoGenerator should create.
     * @param randomSeed
     *          The starting seed that Jetedge should use when creating your objects.  POJO generation is handled in a way to guarantee exact
     *          duplication if provided with the same seed and class.
     */
    public PojoGeneratorBuilder(Class<T> clazz, int randomSeed) {
        this(clazz, randomSeed, Analyzers.DEFAULT);
    }

    /**
     * Creates a builder object for the PojoGenerator with the defined class
     * @param clazz
     *          The class of objects that the PojoGenerator should create.
     * @param randomSeed
     *          The starting seed that Jetedge should use when creating your objects.  POJO generation is handled in a way to guarantee exact
     *          duplication if provided with the same seed and class.
     * @param pojoAnalyzer
     *          A predefined way to analyze your POJOs structure and determine how to build.
     */
    public PojoGeneratorBuilder(Class<T> clazz, int randomSeed, PojoAnalyzer pojoAnalyzer) {
        this(clazz, new PojoAttributes<>(clazz, pojoAnalyzer, randomSeed));
    }

    /**
     * This method should only be used to define system wide defaults.  This property can only be set once, and ALL generators EVERYWHERE
     *      will use the defaults provided here.  The properties can be individually overwritten, as always, but this allows users to
     *      set very basic limiters for the entire system.
     * @throws RuntimeException
     *      If this field is called more than once, a RuntimeException will be thrown
     * @param defaultDataLimiter
     */
    public static void setAllGeneratorsDefaultDataGenerators(DefaultDataLimiter defaultDataLimiter) {
        if (baseDataLimiters != null) {
            // TODO: 1/25/18 Replace this with a specific error and update javadoc.
            throw new RuntimeException("The PojoGeneratorBuilder has already been seeded with system wide base limiters");
        }
        baseDataLimiters = defaultDataLimiter;
    }

    /**
     * Declares what values are acceptable for a specific field.
     * @param fieldName
     *          The name of the field that the following limitations should be enforced on.
     * @param limiter
     *          What range of data is to be used when generating an object.
     * @return A reference of this object
     */
    public PojoGeneratorBuilder<T> andLimitField(String fieldName, Limiter<?> limiter) {
        pojoAttributes.putFieldLimiter(fieldName, limiter);
        return this;
    }

    /**
     * Declares what values are acceptable for a specific field.
     * @param fieldName
     *          The name of the field that the following limitations should be enforced on.
     * @return A reference of this object
     */
    public PojoGeneratorBuilder<T> andIgnoreField(String fieldName) {
        pojoAttributes.ignoreField(fieldName);
        return this;
    }

    /**
     * This method defines what the default behaviour should be for a given data type.  By default, all of the most permissive Limiters are used.
     *      With this method, a new base line can be set so as to avoid having to do basic limitations for every single field.
     *      As an example, the standard IntLimiter effectively acts as <code>Random.nextInt()</code> for every integer found in a given class
     *      structure.  Using this method, the default behaviour for <code>this</code> generator can be adjusted for <code>Random.nextInt(10)</code>
     * @param limiter
     *          The new default limiter (type will automatically be inferred)
     * @return a reference of this object
     */
    public PojoGeneratorBuilder<T> andLimitAllFieldsOf(Limiter<?> limiter) {
        Class clazz = ((Class) ((ParameterizedType) limiter.getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0]);
        pojoAttributes.putAllFieldLimiter(clazz, limiter);
        return this;
    }

    /**
     * This method allows the user to define a collection of Limiters to be used in place of the standard Limiters.  In effect, it
     *      repetitively calls the <code>andLimitAllFieldsOf()</code> method with the provided collection of limiters.  This allows for
     *      reuse of the same default limiters in multiple generators without having to reapply all of the defaults.
     * @param defaultDataLimiter
     *          A collection of limiters to be used in place of the standard default data generators
     * @return a reference of this object
     */
    public PojoGeneratorBuilder<T> setDefaultDataGenerators(DefaultDataLimiter defaultDataLimiter) {
        defaultDataLimiter.getDefaultLimiters().forEach(this::andLimitAllFieldsOf);
        return this;
    }

    /**
     * Defines an analyzer that should be used to go through the class structure and find objects to be populated.
     * @param pojoAnalyzer
     *          How a POJO should be analyzed.  By default, this is done via getters/setters.
     * @return a reference of this object
     * @see Analyzers for a list provided PojoAnalyzers
     */
    public PojoGeneratorBuilder<T> andUseAnalyzer(PojoAnalyzer pojoAnalyzer) {
        pojoAttributes.setPojoAnalyzer(pojoAnalyzer);
        return this;
    }

    /**
     * Defines a random seed to be used for POJO creation.  This same effect can be realized by providing a random seed in the constructor.
     * @param randomSeed
     *          The starting seed that Jetedge should use when creating your objects.  POJO generation is handled in a way to guarantee exact
     *          duplication if provided with the same seed and class.
     * @return a reference of this object
     */
    public PojoGeneratorBuilder<T> andUseRandomSeed(int randomSeed) {
        pojoAttributes.setRandomSeed(randomSeed);
        return this;
    }

    /**
     * This method will make Jetedge only evaluate fields that have been specified by limiters.  All other will be left uninstantiated.
     * @return a reference of this object
     */
    public PojoGeneratorBuilder<T> lazilyEvaluate() {
        pojoAttributes.setEvaluationState(false);
        return this;
    }

    /**
     * This method allows the user to pass in objects to be used in the construction of their object.  It is highly recommended for the
     *      user to use this in conjunction with <code>lazilyEvaluate()</code> or appropriate <code>andIgnoreField()</code> in order to
     *      prevent their values from being overwritten by Jetedge
     * @param objects
     * @return a reference of this object
     */
    public PojoGeneratorBuilder<T> withConstructors(Object... objects) {
        List<ConstructorParameter> parameters = new ArrayList<>();
        for (Object o : objects) {
            parameters.add(new ConstructorParameter(o));
        }
        pojoAttributes.setConstructorObjectList(parameters);
        return this;
    }

    /**
     * Creates the PojoGenerator specified by the fluent API used in the Builder
     * @param classAttributes
     *          A <code>ClassAttributes</code> object to be used for analysis.
     * @return PojoGenerator object for generating objects of type <code>T</code>
     */
    public PojoGenerator<T> build(ClassAttributes classAttributes) {
        pojoAttributes.getParentPojoAnalyzer().extractFields(classAttributes)
                .filter(f -> filterFields(classAttributes, f))
                .forEach(this::createFieldSetters);

        new FieldDataGenerator<T>(pojoAttributes.getRandomSeed()).populateSuppliers(pojoAttributes); // TODO: 11/24/17 Look at making this static

        return () -> {
            try {
                T newInstance = classAttributes.<T>newInstance(pojoAttributes.getConstructorObjectList());
                pojoAttributes.apply(newInstance);
                return newInstance;
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            }
        };
    }

    /**
     * Creates the PojoGenerator specified by the fluent API used in the Builder
     * @return PojoGenerator object for generating objects of type <code>T</code>
     */
    public PojoGenerator<T> build() {
        return this.build(ClassAttributes.create(clazz));
    }

    private void createFieldSetters(FieldAttributes f) {
        pojoAttributes.putFieldSetter(f.getName(), FieldSetter.create(f.getType(), f.getSetter()));
    }

    private boolean filterFields (ClassAttributes classAttributes, FieldAttributes fieldAttributes) {
        String classPrepender = classAttributes.getFieldNameOfClass();
        String fieldName = fieldAttributes.getName().toLowerCase();
        String qualifiedFieldName = !classPrepender.isEmpty() ? classPrepender + "." + fieldName : fieldName;
        return !pojoAttributes.shouldIgnore(qualifiedFieldName);
    }

    /**
     * Clones this PojoGeneratorBuilder such that using the <code>build()</code> operation on both the original and the
     *      clone will yield 2 generators that will create identical POJOs.
     * @return A clone of this object.
     */
    @Override
    public PojoGeneratorBuilder<T> clone() {
        return new PojoGeneratorBuilder<>(clazz, pojoAttributes.clone());
    }
}
