package com.windforce.common.resource.excel;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.windforce.common.resource.anno.Id;
import com.windforce.common.resource.anno.Index;
import com.windforce.common.resource.anno.Resource;

@Resource
public class Human {

	public static final String INDEX_NAME = "human_name";
	public static final String INDEX_AGE = "human_age";

	public static final class HumanComparator implements Comparator<Human> {
		@Override
		public int compare(Human o1, Human o2) {
			return -o1.id.compareTo(o2.id);
		}
	};

	@Id
	private Integer id;
	@Index(name = INDEX_NAME, unique = true)
	private String name;
	@Index(name = INDEX_AGE, comparatorClz = HumanComparator.class)
	private int age;
	private boolean sex;
	private List<Pet> petList;
	private Map<Integer, Pet> petMap;
	private Pet pet;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public boolean isSex() {
		return sex;
	}

	public void setSex(boolean sex) {
		this.sex = sex;
	}

	public List<Pet> getPetList() {
		return petList;
	}

	public void setPetList(List<Pet> petList) {
		this.petList = petList;
	}

	public Map<Integer, Pet> getPetMap() {
		return petMap;
	}

	public void setPetMap(Map<Integer, Pet> petMap) {
		this.petMap = petMap;
	}

	public Pet getPet() {
		return pet;
	}

	public void setPet(Pet pet) {
		this.pet = pet;
	}

}
