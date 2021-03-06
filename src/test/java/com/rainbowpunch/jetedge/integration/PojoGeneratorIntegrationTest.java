package com.rainbowpunch.jetedge.integration;

import com.rainbowpunch.jetedge.core.limiters.collections.ListLimiter;
import com.rainbowpunch.jetedge.core.limiters.common.ConstantValueLimiter;
import com.rainbowpunch.jetedge.core.limiters.primitive.IntegerLimiter;
import com.rainbowpunch.jetedge.core.limiters.primitive.StringLimiter;
import com.rainbowpunch.jetedge.core.limiters.special.MultiplexLimiter;
import com.rainbowpunch.jetedge.spi.DefaultDataLimiter;
import com.rainbowpunch.jetedge.spi.PojoGenerator;
import com.rainbowpunch.jetedge.spi.PojoGeneratorBuilder;
import com.rainbowpunch.jetedge.test.Pojos;
import com.rainbowpunch.jetedge.test.Pojos.Extra;
import com.rainbowpunch.jetedge.test.Pojos.Person;
import com.rainbowpunch.jetedge.test.Pojos.Superhero;
import com.rainbowpunch.jetedge.test.Pojos.Vehicle;

import com.rainbowpunch.jetedge.util.ReadableCharList;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static com.rainbowpunch.jetedge.test.Assertions.assertPojosShallowEqual;
import static com.rainbowpunch.jetedge.test.Pojos.*;
import static com.rainbowpunch.jetedge.test.Pojos.Power.*;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PojoGeneratorIntegrationTest {
    private static final int RANDOM_SEED = 42;

    @Test
    public void testGenerateEmptyPojo() {
        Extra generated = new PojoGeneratorBuilder<>(Extra.class)
                .build()
                .generatePojo();

        assertNotNull(generated);
    }

    @Test
    public void testGenerateBasicPojo() {
        Person generated = new PojoGeneratorBuilder<>(Person.class)
                .andUseRandomSeed(RANDOM_SEED)
                .build()
                .generatePojo();

        assertEquals(-1771985870, generated.getAge());
        assertEquals("ya,f,0(TDja_(!DkOIfD[$(ntus7.1", generated.getName());
    }

    @Test
    public void testGeneratePojoWithInheritedFields() {
        Superhero generated = new PojoGeneratorBuilder<>(Superhero.class)
                .andUseRandomSeed(RANDOM_SEED)
                .build()
                .generatePojo();

        assertEquals(389401069, generated.getAge());
        assertEquals("ya,f,0(TDja_(!DkOIfD[$(ntus7.1", generated.getName());

        // also verify that direct fields are picked up
        assertNotNull(generated.getSuperPowers());
        assertNotNull(generated.getArchNemesis());
    }

    @Test
    public void testGeneratePojoWithListField() {
        Superhero generated = new PojoGeneratorBuilder<>(Superhero.class)
                .andUseRandomSeed(RANDOM_SEED)
                .build()
                .generatePojo();

        List<Pojos.Power> powers = asList(SPEED, SPEED, FLIGHT, MONEY, SPIDER_SENSE, FLIGHT);
        assertEquals(powers, generated.getSuperPowers());
    }

    @Test
    @Ignore("nested POJO does not currently inherit random seed")
    public void testGeneratePojoWithNestedPojo() {
        Person generated = new PojoGeneratorBuilder<>(Superhero.class)
                .andUseRandomSeed(RANDOM_SEED)
                .build()
                .generatePojo()
                .getArchNemesis();

        assertEquals(0, generated.getAge());
        assertEquals("", generated.getName());
    }

    @Test
    @Ignore
    public void testPojoGeneratorBuilderClone() {
        PojoGeneratorBuilder<Person> baseGen = new PojoGeneratorBuilder<>(Person.class);

        Person generatedA = baseGen.clone().build().generatePojo();
        Person generatedB = baseGen.clone().build().generatePojo();

        assertPojosShallowEqual(generatedA, generatedB);
    }

    @Test
    public void testLimitFieldByName() {
        int expectedLength = 4;
        Person generated = new PojoGeneratorBuilder<>(Person.class)
                .andUseRandomSeed(RANDOM_SEED)
                .andLimitField("name", new StringLimiter(expectedLength))
                .build()
                .generatePojo();

        assertEquals(expectedLength, generated.getName().length());
    }

    @Test
    public void testLimitAllFieldsOfType() {
        int expectedRange = 10;
        Vehicle generated = new PojoGeneratorBuilder<>(Vehicle.class)
                .andUseRandomSeed(RANDOM_SEED)
                .andLimitAllFieldsOf(new IntegerLimiter(expectedRange))
                .build()
                .generatePojo();

        assertTrue(expectedRange >= generated.getMaxSpeed());
        assertTrue(expectedRange >= generated.getNumWheels());
    }

    @Test
    public void testUseCustomAnalyzer() {
        Person generated = new PojoGeneratorBuilder<>(Person.class)
                .andUseRandomSeed(RANDOM_SEED)
                // use a custom analyzer that only includes string attributes
                .andUseAnalyzer(classAttributes ->
                        classAttributes.getFields().stream()
                                .filter(f -> f.getType().is(String.class)))
                .build()
                .generatePojo();

        assertEquals("h1<t\"c!>ya,f,0(TDja_(!DkOIfD[$", generated.getName());
        assertEquals(0, generated.getAge());
    }

    @Test
    public void testIgnoreField() {
        Vehicle generated = new PojoGeneratorBuilder<>(Vehicle.class)
                .andIgnoreField("name")
                .andIgnoreField("maxSpeed")
                .build()
                .generatePojo();
        assertNull(generated.getName());
        assertEquals(0, generated.getMaxSpeed());
        assertNotNull(generated.getNumWheels());
    }

    @Test
    public void testNestedIgnoreField() {
        PojoGenerator<Storyline> generator = new PojoGeneratorBuilder<>(Storyline.class)
                .andIgnoreField("archNemesis.name")
                .andIgnoreField("superhero.superPowers")
                .andIgnoreField("superhero.archNemesis.age")
                .build();

        Storyline generated = generator.generatePojo();

        assertNull(generated.getArchNemesis().getName());
        assertNull(generated.getSuperhero().getSuperPowers());
        assertEquals(0, generated.getSuperhero().getArchNemesis().getAge());
    }

    @Test
    public void testNestedLimiter() {
        PojoGenerator<Storyline> generator = new PojoGeneratorBuilder<>(Storyline.class)
                .andLimitField("archNemesis.name", new ConstantValueLimiter<String>("Johnny"))
                .andLimitField("superhero.superPowers", new ListLimiter(3, 12))
                .andLimitField("superhero.archNemesis.age", new IntegerLimiter(100))
                .build();

        Storyline generated = generator.generatePojo();

        assertEquals("Johnny", generated.getArchNemesis().getName());
        assertTrue(generated.getSuperhero().getSuperPowers().size() >= 12);
        assertTrue(generated.getSuperhero().getArchNemesis().getAge() <= 100);
    }

    @Test
    public void testGenericInterfaceBeingPopulated() {
        PojoGenerator<B> generator = new PojoGeneratorBuilder<>(B.class)
                .build();
        B generated = generator.generatePojo();
        assertNotNull(generated);
        assertNotNull(generated.getJ());
    }

    @Test
    public void testMultiplexLimiter() {
        List<IntegerLimiter> limiters = Arrays.asList(new IntegerLimiter(10), new IntegerLimiter(10, 20), new IntegerLimiter(10, 40));

        PojoGenerator<Person> generator = new PojoGeneratorBuilder<>(Person.class)
                .andLimitField("age", MultiplexLimiter.generateFlatDistribution(limiters))
                .build();

        for (int i = 0; i < 10000; i++) {
            Person person = generator.generatePojo();
            int pAge = person.getAge();
            assertTrue("Bad Value: " + String.valueOf(person.getAge()),
                    (pAge >= 0 && pAge < 10) || (pAge >= 20 && pAge < 30) || (pAge >= 40 && pAge < 50));
        }
    }

    @Test
    public void testLazyEvaluation_noPopulation() {
        PojoGenerator<Vehicle> generator = new PojoGeneratorBuilder<>(Vehicle.class)
                .lazilyEvaluate()
                .build();

        Vehicle vehicle = generator.generatePojo();

        assertNotNull(vehicle);
        assertEquals(0, vehicle.getMaxSpeed());
        assertEquals(0, vehicle.getNumWheels());
        assertNull(vehicle.getName());
        assertNull(vehicle.getEngineType());
    }

    @Test
    public void testLazyEvaluation_partialPopulation() {
        PojoGenerator<Vehicle> generator = new PojoGeneratorBuilder<>(Vehicle.class)
                .lazilyEvaluate()
                .andLimitField("name", new ConstantValueLimiter<>("Hello World"))
                .build();

        Vehicle vehicle = generator.generatePojo();

        assertNotNull(vehicle);
        assertEquals(0, vehicle.getMaxSpeed());
        assertEquals(0, vehicle.getNumWheels());
        assertEquals("Hello World", vehicle.getName());
        assertNull(vehicle.getEngineType());
    }

    @Test
    public void testSetDefaultDataGenerators() {
        DefaultDataLimiter limiters = new DefaultDataLimiter();
        limiters.addDefaultLimiter(new IntegerLimiter(10, 10));
        limiters.addDefaultLimiter(new StringLimiter(ReadableCharList.LIST_OF_ALPHA_CHAR));

        PojoGenerator<Vehicle> generator = new PojoGeneratorBuilder<>(Vehicle.class)
                .setDefaultDataGenerators(limiters)
                .build();

        Vehicle vehicle = generator.generatePojo();

        assertTrue(vehicle.getMaxSpeed() > 9 && vehicle.getMaxSpeed() < 20);
        assertTrue(vehicle.getNumWheels() > 9 && vehicle.getNumWheels() < 20);
        Arrays.asList(vehicle.getName().toCharArray()).forEach(ReadableCharList.LIST_OF_ALL_CHAR::contains);
        assertNotNull(vehicle.getEngineType());
    }

    @Test
    public void testSetDefaultDataGenerators_overWritten() {
        DefaultDataLimiter limiters = new DefaultDataLimiter();
        limiters.addDefaultLimiter(new IntegerLimiter(10, 10));
        limiters.addDefaultLimiter(new StringLimiter(ReadableCharList.LIST_OF_ALPHA_CHAR));

        PojoGenerator<Vehicle> generator = new PojoGeneratorBuilder<>(Vehicle.class)
                .setDefaultDataGenerators(limiters)
                .andLimitAllFieldsOf(new StringLimiter(ReadableCharList.LIST_OF_CHAR_DIGITS))
                .build();

        Vehicle vehicle = generator.generatePojo();

        assertTrue(vehicle.getMaxSpeed() > 9 && vehicle.getMaxSpeed() < 20);
        assertTrue(vehicle.getNumWheels() > 9 && vehicle.getNumWheels() < 20);
        Arrays.asList(vehicle.getName().toCharArray()).forEach(ReadableCharList.LIST_OF_CHAR_DIGITS::contains);
        assertNotNull(vehicle.getEngineType());
    }

    @Test
    public void testGenerateObjectWithoutDefaultConstructor() {
        PojoGenerator<ParameterConstructor> generator = new PojoGeneratorBuilder<>(ParameterConstructor.class)
                .withConstructors(78, "Hello world")
                .andIgnoreField("someNumber")
                .andIgnoreField("someString")
                .build();

        ParameterConstructor constructor = generator.generatePojo();

        assertEquals(78, constructor.getSomeNumber());
        assertEquals("Hello world", constructor.getSomeString());
        assertTrue(constructor.getSomeRandomNumber() != 0);
        assertTrue(!constructor.getSomeRandomString().isEmpty());

    }

    @Test
    public void testGenerateObjectWithoutDefaultConstructor_int() {
        PojoGenerator<ParameterConstructor> generator = new PojoGeneratorBuilder<>(ParameterConstructor.class)
                .withConstructors(78)
                .andIgnoreField("someNumber")
                .build();

        ParameterConstructor constructor = generator.generatePojo();

        assertEquals(78, constructor.getSomeNumber());
        assertTrue(constructor.getSomeRandomNumber() != 0);
        assertTrue(!constructor.getSomeString().isEmpty());
        assertTrue(!constructor.getSomeRandomString().isEmpty());

    }
    @Test
    public void testGenerateObjectWithoutDefaultConstructor_string() {
        PojoGenerator<ParameterConstructor> generator = new PojoGeneratorBuilder<>(ParameterConstructor.class)
                .withConstructors("Hello world")
                .andIgnoreField("someString")
                .build();

        ParameterConstructor constructor = generator.generatePojo();

        assertEquals("Hello world", constructor.getSomeString());
        assertTrue(constructor.getSomeNumber() != 0);
        assertTrue(constructor.getSomeRandomNumber() != 0);
        assertTrue(!constructor.getSomeRandomString().isEmpty());

    }
}
